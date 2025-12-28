package com.maltson.launchdarklyobservabilitypoc;

import com.launchdarkly.openfeature.serverprovider.Provider;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.OpenFeatureAPI;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenFeatureConfig {

    @Bean
    public OpenFeatureAPI openFeatureAPI() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String sdkKey = dotenv.get("LAUNCHDARKLY_SDK_KEY");
        if (sdkKey == null) {
            sdkKey = System.getenv("LAUNCHDARKLY_SDK_KEY");
        }

        if (sdkKey != null && !sdkKey.isEmpty()) {
            Provider provider = new Provider(sdkKey);
            OpenFeatureAPI.getInstance().setProviderAndWait(provider);
        } else {
            System.out.println("LAUNCHDARKLY_SDK_KEY not found. Using default NoOpProvider.");
        }

        return OpenFeatureAPI.getInstance();
    }

    @Bean
    public Client client(OpenFeatureAPI openFeatureAPI) {
        return openFeatureAPI.getClient();
    }
}
