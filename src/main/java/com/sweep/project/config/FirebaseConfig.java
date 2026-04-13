package com.sweep.project.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

// firebase.config-path 설정이 없으면 빈 등록 X
@Configuration
@ConditionalOnProperty(name = "firebase.config-path", matchIfMissing = false)
public class FirebaseConfig {

    @Value("${firebase.config-path}")
    private String configPath;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        Path path = Path.of(configPath);
        if (!Files.exists(path)) {
            throw new IllegalStateException("firebase 설정 파일을 찾을 수 없습니다: " + configPath);
        }

        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getApps().getFirst();
        }

        try (InputStream inputStream = Files.newInputStream(path)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(inputStream))
                    .build();

            return FirebaseApp.initializeApp(options);
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
