package com.maltson.launchdarklyobservabilitypoc;

import io.github.cdimascio.dotenv.Dotenv;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
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
        AttributesBuilder resourceAttributes = Attributes.builder()
                .put("service.name", serviceName);

        if (sdkKey != null && !sdkKey.isEmpty()) {
            resourceAttributes.put("highlight.project_id", sdkKey);
        }

        Resource resource = Resource.create(resourceAttributes.build());

        // Ensure endpoint has the correct path for traces
        String tracesEndpoint = otlpEndpoint;
        if (!tracesEndpoint.endsWith("/v1/traces")) {
            tracesEndpoint = tracesEndpoint + "/v1/traces";
        }

        // Configure OTLP exporter (HTTP)
        OtlpHttpSpanExporter spanExporter = OtlpHttpSpanExporter.builder()
                .setEndpoint(tracesEndpoint)
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
