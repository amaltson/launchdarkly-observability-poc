package com.maltson.launchdarklyobservabilitypoc;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.MutableContext;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.time.LocalTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {

	private static final String template = "Hello, %s!";
    private final Client openFeatureClient;
    private final Tracer tracer;

    public GreetingController(Client openFeatureClient, Tracer tracer) {
        this.openFeatureClient = openFeatureClient;
        this.tracer = tracer;
    }

	@GetMapping("/greeting")
	public String greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        // Create a span for this greeting request
        Span span = tracer.spanBuilder("greeting.request").startSpan();

        try (Scope scope = span.makeCurrent()) {
            // Create an anonymous evaluation context
            EvaluationContext context = new MutableContext("anonymous-user");

            // Evaluate the feature flag
            boolean isElegantGreeting = openFeatureClient.getBooleanValue("elegant-greeting", false, context);

            // Add span attributes
            span.setAttribute("greeting.name", name);
            span.setAttribute("feature.flag.elegant-greeting", isElegantGreeting);

            String response;
            if (isElegantGreeting) {
                String timeOfDay = getTimeOfDay();
                span.setAttribute("greeting.type", "elegant");
                span.setAttribute("greeting.time_of_day", timeOfDay);
                response = getElegantGreeting(name);
            } else {
                span.setAttribute("greeting.type", "regular");
                response = String.format(template, name);
            }

            return response;
        } finally {
            span.end();
        }
	}

    private String getTimeOfDay() {
        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.NOON)) {
            return "morning";
        } else if (now.isBefore(LocalTime.of(18, 0))) {
            return "afternoon";
        } else {
            return "evening";
        }
    }

    private String getElegantGreeting(String name) {
        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.NOON)) {
            return String.format("Top of the morning, %s!", name);
        } else if (now.isBefore(LocalTime.of(18, 0))) {
            return String.format("Good afternoon, %s. A pleasure to see you.", name);
        } else {
            return String.format("Good evening, %s. I hope you had a splendid day.", name);
        }
    }
}
