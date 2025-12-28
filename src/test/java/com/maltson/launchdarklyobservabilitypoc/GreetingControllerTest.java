package com.maltson.launchdarklyobservabilitypoc;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.OpenFeatureAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GreetingController.class)
@ContextConfiguration(classes = {GreetingController.class, GreetingControllerTest.TestConfig.class})
class GreetingControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private Client client;

	@Configuration
	static class TestConfig {
		@Bean
		public OpenFeatureAPI openFeatureAPI() {
			return OpenFeatureAPI.getInstance();
		}

		@Bean
		public Client client() {
			return mock(Client.class);
		}
	}

	@BeforeEach
	void setUp() {
		// Client is already mocked via TestConfig
	}

	@Test
	void shouldReturnDefaultMessage() throws Exception {
		when(client.getBooleanValue(eq("elegant-greeting"), anyBoolean(), any())).thenReturn(false);

		this.mockMvc.perform(get("/greeting"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Hello, World!")));
	}

	@Test
	void shouldReturnTailoredMessage() throws Exception {
		when(client.getBooleanValue(eq("elegant-greeting"), anyBoolean(), any())).thenReturn(false);

		this.mockMvc.perform(get("/greeting").param("name", "Spring Community"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Hello, Spring Community!")));
	}

	@Test
	void shouldReturnElegantMessage() throws Exception {
		when(client.getBooleanValue(eq("elegant-greeting"), anyBoolean(), any())).thenReturn(true);

		// Since we can't easily control time in this simple setup, we check for any of the valid greetings
		this.mockMvc.perform(get("/greeting").param("name", "My Lord"))
				.andExpect(status().isOk())
				.andExpect(content().string(anyOf(
						containsString("Top of the morning, My Lord!"),
						containsString("Good afternoon, My Lord. A pleasure to see you."),
						containsString("Good evening, My Lord. I hope you had a splendid day.")
				)));
	}
}
