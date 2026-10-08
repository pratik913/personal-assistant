package com.personalassistant.ai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.stereotype.Component;

@Component
public class OpenAiClientFactory {

    public OpenAIClient create(
            String apiKey
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OpenAI API key is not configured."
            );
        }

        return OpenAIOkHttpClient
                .builder()
                .apiKey(apiKey)
                .build();
    }
}