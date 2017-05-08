package am.ik.jdt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class PollerController {
	private static final Logger log = LoggerFactory.getLogger(PollerController.class);
	private final ReactivePoller poller;

	public PollerController(ReactivePoller poller) {
		this.poller = poller;
	}

	@GetMapping("data")
	Flux<Data> data() {
		return poller.dataChannel().mergeWith(poller.dataStream());
	}

	@PostMapping("data/{q}/{a}")
	Mono<Void> poll(@PathVariable String q, @PathVariable String a) {
		Data data = new Data(q, a);
		return poller.poll(data).flatMap(poller::publishData);
	}
}
