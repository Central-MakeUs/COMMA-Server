package com.cmc.comma.global.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * OCI Object Storage 연동 설정.
 * OCI의 S3 호환 엔드포인트에 AWS S3 SDK를 붙여 사용한다 (요청은 전부 OCI로 감).
 */
@Configuration
public class StorageConfig {

    @Value("${oci.storage.endpoint}")
    private String endpoint;

    @Value("${oci.storage.region}")
    private String region;

    @Value("${oci.storage.access-key}")
    private String accessKey;

    @Value("${oci.storage.secret-key}")
    private String secretKey;

    private StaticCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }

    // OCI S3 호환 엔드포인트는 path-style 주소만 지원 → pathStyleAccessEnabled(true) 필수
    private S3Configuration pathStyleConfig() {
        return S3Configuration.builder().pathStyleAccessEnabled(true).build();
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .serviceConfiguration(pathStyleConfig())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .serviceConfiguration(pathStyleConfig())
                .build();
    }
}