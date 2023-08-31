package com.publicis.sapient.p2p.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class CloudConfig {

    @Value("${spring.cloud.project-id}")
    private String projectId;

    @Bean
    public Storage storage() throws IOException {
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
        StorageOptions options = StorageOptions.newBuilder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build();
        return options.getService();
    }

    @Bean
    public ImageAnnotatorClient visionService() throws IOException {

        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
        ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider(() -> credentials)
                .build();
        return ImageAnnotatorClient.create(settings);
    }
}
