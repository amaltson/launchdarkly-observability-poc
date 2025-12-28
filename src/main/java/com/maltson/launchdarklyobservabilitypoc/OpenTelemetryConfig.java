package com.maltson.launchdarklyobservabilitypoc;

import io.github.cdimascio.dotenv.Dotenv;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryConfig {

    @Value("${spring.application.name:launchdarkly-observability-poc}")
    private String serviceName;

    @Bean
    public OpenTelemetry openTelemetry() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        String otlpEndpoint = dotenv.get("OTEL_EXPORTER_OTLP_ENDPOINT");
        if (otlpEndpoint == null) {
            otlpEndpoint = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT");
        }

        String sdkKey = dotenv.get("LAUNCHDARKLY_SDK_KEY");
        if (sdkKey == null) {
            sdkKey = System.getenv("LAUNCHDARKLY_SDK_KEY");
        }

        // If no OTLP endpoint is configured, return a no-op OpenTelemetry instance
        if (otlpEndpoint == null || otlpEndpoint.isEmpty()) {
            System.out.println("OTEL_EXPORTER_OTLP_ENDPOINT not found. OpenTelemetry disabled.");
            return OpenTelemetry.noop();
        }

        System.out.println("Configuring OpenTelemetry with OTLP endpoint: " + otlpEndpoint);

        // Build resource attributes
        io.opentelemetry.api.common.AttributesBuilder resourceAttributes = Attributes.builder()
                .put(ResourceAttributes.SERVICE_NAME, serviceName);

        if (sdkKey != null && !sdkKey.isEmpty()) {
            resourceAttributes.put("launchdarkly.sdk.key", sdkKey);
        }

        Resource resource = Resource.create(resourceAttributes.build());

        // Configure OTLP exporter
        OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint(otlpEndpoint)
                .build();

        // Build tracer provider
        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .setResource(resource)
                .build();

        // Build OpenTelemetry SDK
        return OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .buildAndRegisterGlobal();
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("com.maltson.launchdarklyobservabilitypoc");
    }
}
