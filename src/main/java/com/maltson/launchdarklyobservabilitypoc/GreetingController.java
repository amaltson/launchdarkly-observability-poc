package com.maltson.launchdarklyobservabilitypoc;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.MutableContext;
import java.time.LocalTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {

	private static final String template = "Hello, %s!";
    private final Client openFeatureClient;

    public GreetingController(Client openFeatureClient) {
        this.openFeatureClient = openFeatureClient;
    }

	@GetMapping("/greeting")
	public String greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        // Create an anonymous evaluation context
        EvaluationContext context = new MutableContext("anonymous-user");

        if (openFeatureClient.getBooleanValue("elegant-greeting", false, context)) {
            return getElegantGreeting(name);
        }
		return String.format(template, name);
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
