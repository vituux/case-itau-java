package br.com.vituux.renegociacaopagamentos.config;


import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String region;

    @Value("${aws.credentials.access-key}")
    private String accessKey;

    @Value("${aws.credentials.secret-key}")
    private String secretKey;

    @Value("${aws.endpoint:#{null}}")
    private String endpoint;

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
