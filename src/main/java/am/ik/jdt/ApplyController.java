package am.ik.jdt;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class ApplyController {
	private final Lottery lottery;

	public ApplyController(Lottery lottery) {
		this.lottery = lottery;
	}

	@PostMapping("application")
	Mono<Integer> apply(@RequestBody Mono<Application> application) {
		return application.flatMap(a -> this.lottery.apply(a.name, a.email));
	}

	@DeleteMapping("application")
	Mono<Void> clear() {
		return lottery.clear();
	}

	public static class Application {
		private String name;
		private String email;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}
	}
}
