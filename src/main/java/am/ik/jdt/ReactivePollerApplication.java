package am.ik.jdt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;

@SpringBootApplication
public class ReactivePollerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReactivePollerApplication.class, args);
	}

	@Bean
	public RedisClient redisClient(LettuceConnectionFactory connectionFactory) {
		RedisURI.Builder builder = RedisURI.builder()
				.withHost(connectionFactory.getHostName())
				.withPort(connectionFactory.getPort());
		if (connectionFactory.getPassword() != null) {
			builder.withPassword(connectionFactory.getPassword());
		}
		return RedisClient.create(builder.build());
	}
}
