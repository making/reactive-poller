package am.ik.jdt;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.reactive.ChannelMessage;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

@Component
public class ReactivePoller {
	private static final Logger log = LoggerFactory.getLogger(ReactivePoller.class);
	private final RedisClient redisClient;
	private StatefulRedisConnection<String, String> dataConnection;
	private StatefulRedisConnection<String, String> userConnection;
	private StatefulRedisPubSubConnection<String, String> dataConnectionSub;
	private StatefulRedisPubSubConnection<String, String> userConnectionSub;
	private StatefulRedisPubSubConnection<String, String> userConnectionPub;
	private Flux<Data> dataChannel;
	private Flux<Long> userChannel;

	public ReactivePoller(RedisClient redisClient) {
		this.redisClient = redisClient;
	}

	@PostConstruct
	void init() {
		log.info("Connected to Redis");
		this.dataConnection = redisClient.connect();
		this.userConnection = redisClient.connect();
		this.subscribeData();
		this.subscribeUser();
	}

	@PreDestroy
	void destroy() {
		log.info("Destroy");
		this.dataConnection.close();
		this.dataConnectionSub.close();
		this.userConnection.close();
		this.userConnectionSub.close();
		this.userConnectionPub.close();
	}

	private void subscribeData() {
		this.dataConnectionSub = redisClient.connectPubSub();
		RedisPubSubReactiveCommands<String, String> commandsPubSub = this.dataConnectionSub
				.reactive();
		commandsPubSub.subscribe("q1", "q2", "q3", "q4").subscribe();
		this.dataChannel = commandsPubSub.observeChannels(FluxSink.OverflowStrategy.DROP)
				.flatMap(this::handleDataMessage).share();
		this.dataChannel.log("data-channel").subscribe();
	}

	private void subscribeUser() {
		this.userConnectionSub = this.redisClient.connectPubSub();
		this.userConnectionPub = this.redisClient.connectPubSub();
		RedisPubSubReactiveCommands<String, String> commandsPubSub = this.userConnectionSub
				.reactive();
		commandsPubSub.subscribe("user").subscribe();
		this.userChannel = commandsPubSub.observeChannels(FluxSink.OverflowStrategy.DROP)
				.flatMap(this::handleUserMessage).share();
		this.userChannel.log("user-channel").subscribe();

	}

	private Mono<Data> handleDataMessage(ChannelMessage<String, String> message) {
		RedisReactiveCommands<String, String> commands = this.dataConnection.reactive();
		Data data = new Data(message.getChannel(), message.getMessage());
		return commands.get(data.key()).map(Long::valueOf).map(data::withCount);
	}

	private Mono<Long> handleUserMessage(ChannelMessage<String, String> message) {
		RedisReactiveCommands<String, String> commands = this.userConnection.reactive();
		return commands.get("user").map(Long::valueOf);
	}

	public Mono<Void> publishData(Data data) {
		RedisPubSubReactiveCommands<String, String> commands = this.userConnectionPub
				.reactive();
		return commands.publish(data.getQuestion(), data.getAnswer()).then();
	}

	public Mono<Void> publishUser(String message) {
		RedisPubSubReactiveCommands<String, String> commands = this.userConnectionPub
				.reactive();
		switch (message) {
		case "+":
			return commands.incr("user").map(String::valueOf)
					.flatMap(v -> commands.publish("user", v)).then();
		case "-":
			return commands.decr("user").map(String::valueOf)
					.flatMap(v -> commands.publish("user", v)).then();
		default:
			return Mono.empty();
		}
	}

	public Flux<Data> dataStream() {
		RedisReactiveCommands<String, String> commands = this.dataConnection.reactive();
		return commands.keys("q*:a*").flatMap(
				k -> commands.get(k).map(v -> Data.fromKeyAndCount(k, Long.valueOf(v))));
	}

	public Mono<Data> poll(Data data) {
		RedisReactiveCommands<String, String> commands = this.dataConnection.reactive();
		return commands.incr(data.key()).map(data::withCount);
	}

	public Flux<Data> dataChannel() {
		return this.dataChannel;
	}

	public Flux<Long> userChannel() {
		return this.userChannel;
	}
}
