package com.fatmaemran.blueprints.catalog.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Profile("dev")
@RequestMapping("/internal/dev")
public class SlowOperationDemoController {
    private static final long SIMULATED_DELAY_MILLIS = 1_500;
    private final ObservationRegistry observationRegistry;

    public SlowOperationDemoController(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    @GetMapping("/slow")
    public Map<String, Object> simulateSlowOperation() {
        Observation observation = Observation.createNotStarted(
                        "catalog.demo.slow-operation", observationRegistry)
                .lowCardinalityKeyValue("scenario", "intentional-delay");
        observation.start();
        try {
            Thread.sleep(SIMULATED_DELAY_MILLIS);
            return Map.of(
                    "message", "Development-only slow operation completed",
                    "simulatedDelayMillis", SIMULATED_DELAY_MILLIS
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            observation.error(exception);
            throw new IllegalStateException("Slow-operation demo was interrupted", exception);
        } catch (RuntimeException exception) {
            observation.error(exception);
            throw exception;
        } finally {
            observation.stop();
        }
    }
}
