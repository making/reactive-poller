package am.ik.jdt;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class Lottery {
	private final JdbcTemplate jdbcTemplate;
	private final SimpleJdbcInsert insert;

	public Lottery(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.insert = new SimpleJdbcInsert(jdbcTemplate) //
				.withTableName("lottery") //
				.usingGeneratedKeyColumns("number");
	}

	public Mono<Integer> apply(String name, String email) {
		MapSqlParameterSource source = new MapSqlParameterSource() //
				.addValue("name", name) //
				.addValue("email", email);

		return Mono.just(source) //
				.publishOn(Schedulers.elastic()) //
				.map(s -> this.insert.executeAndReturnKey(source).intValue());
	}

	public Mono<Void> clear() {
		return Mono.fromCallable(() -> jdbcTemplate.update("DELETE FROM lottery"))
				.subscribeOn(Schedulers.elastic()).then();
	}
}
