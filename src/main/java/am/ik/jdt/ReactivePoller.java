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
	private StatefulRedisPubSubConnection<String, String> dataConnectionPub;
	private StatefulRedisPubSubConnection<String, String> dataConnectionSub;
	private Flux<Data> dataChannel;

	public ReactivePoller(RedisClient redisClient) {
		this.redisClient = redisClient;
	}

	@PostConstruct
	void init() {
		log.info("Connected to Redis");
		this.dataConnection = redisClient.connect();
		this.subscribeData();
	}

	@PreDestroy
	void destroy() {
		log.info("Destroy");
		this.dataConnection.close();
		this.dataConnectionPub.close();
		this.dataConnectionSub.close();
	}

	private void subscribeData() {
		this.dataConnectionSub = redisClient.connectPubSub();
		this.dataConnectionPub = redisClient.connectPubSub();
		RedisPubSubReactiveCommands<String, String> commandsPubSub = this.dataConnectionSub
				.reactive();
		commandsPubSub.subscribe("q1", "q2", "q3", "q4").subscribe();
		this.dataChannel = commandsPubSub.observeChannels(FluxSink.OverflowStrategy.DROP)
				.flatMap(this::handleDataMessage).share();
		this.dataChannel.log("data-channel").subscribe();
	}

	private Mono<Data> handleDataMessage(ChannelMessage<String, String> message) {
		RedisReactiveCommands<String, String> commands = this.dataConnection.reactive();
		Data data = new Data(message.getChannel(), message.getMessage());
		return commands.get(data.key()).map(Long::valueOf).map(data::withCount);
	}

	public Mono<Void> publishData(Data data) {
		RedisPubSubReactiveCommands<String, String> commands = this.dataConnectionPub
				.reactive();
		return commands.publish(data.getQuestion(), data.getAnswer()).then();
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
}
