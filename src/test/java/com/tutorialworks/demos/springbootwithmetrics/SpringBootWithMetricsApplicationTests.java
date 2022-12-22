package com.tutorialworks.demos.springbootwithmetrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests to check that metrics are rendered.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SpringBootWithMetricsApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

	@Autowired
	MeterRegistry registry;

	/**
	 * Sends a test request to the Greeting API, then asserts that the counter
	 * has increased to 1.
	 * @throws InterruptedException
	 */
    @Test
    void testMetricsRequestCounterIncreases() throws InterruptedException {
		// Make a test request to the Greeting service
		ResponseEntity<String> response = this.restTemplate.getForEntity("/greeting", String.class);

		assertThat(registry.get("greetings.counter").counter().count()).isEqualTo(1);
    }

}
