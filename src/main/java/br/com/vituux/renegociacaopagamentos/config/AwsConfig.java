package br.com.vituux.renegociacaopagamentos.config;


import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String region;

    @Value("${aws.credentials.access-key}")
    private String accessKey;

    @Value("${aws.credentials.secret-key}")
    private String secretKey;

    @Value("${aws.endpoint:}")
    private String endpoint;


    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .slidingWindowSize(10)
                .permittedNumberOfCallsInHalfOpenState(5)
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(1000))
                .build();

        return RetryRegistry.of(config);
    }

    @Bean
    public AmazonCloudWatch amazonCloudWatch() {
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        if (endpoint != null && !endpoint.isEmpty()) {
            // Configuração para LocalStack
            return AmazonCloudWatchClientBuilder.standard()
                    .withEndpointConfiguration(
                            new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .build();
        } else {
            return AmazonCloudWatchClientBuilder.standard()
                    .withRegion(region)
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .build();
        }
    }
    @Bean
    public AmazonS3 amazonS3() {
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        if (endpoint != null && !endpoint.isEmpty()) {
            // Configuração para LocalStack
            return AmazonS3ClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .withPathStyleAccessEnabled(true)
                    .build();
        } else {
            // Configuração para AWS real
            return AmazonS3ClientBuilder.standard()
                    .withRegion(region)
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .build();
        }
    }

    @Bean
    public AmazonSNS amazonSNS() {
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        if (endpoint != null && !endpoint.isEmpty()) {
            // Configuração para LocalStack
            return AmazonSNSClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .build();
        } else {
            // Configuração para AWS real
            return AmazonSNSClientBuilder.standard()
                    .withRegion(region)
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .build();
        }
    }

    @Bean
    public AmazonSQS amazonSQS() {
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        if (endpoint != null && !endpoint.isEmpty()) {
            // Configuração para LocalStack
            return AmazonSQSClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .build();
        } else {
            // Configuração para AWS real
            return AmazonSQSClientBuilder.standard()
                    .withRegion(region)
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .build();
        }
    }
}
