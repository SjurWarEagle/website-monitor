package de.tkunkel.monitor.monitors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.io.IOException;

@Service
public class TelegramMessageSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramMessageSender.class);

    // Use environment variables for sensitive information
    private static final String BOT_TOKEN_ENV_VAR = "TELEGRAM_BOT_TOKEN";
    private static final String CHAT_ID_ENV_VAR = "TELEGRAM_CHAT_ID";

    public static void main(String[] args) {
        // The message you want to send - you could also make this an argument
        String messageText = "Hello from Java running with environment variables!";

        var telegramMessageSender = new TelegramMessageSender();
        telegramMessageSender.sendMessage(messageText);
    }

    private static String getValidateEnv(String chatIdEnvVar) {
        String chatId = System.getenv(chatIdEnvVar);
        if (chatId == null || chatId.trim().isEmpty()) {
            LOGGER.error("Error: Environment variable '" + chatIdEnvVar + "' is not set or is empty.");
            System.exit(1); // Exit with an error code
        }
        return chatId;
    }

    public void sendMessage(String messageText) {
        String botToken = getValidateEnv(BOT_TOKEN_ENV_VAR);
        String chatId = getValidateEnv(CHAT_ID_ENV_VAR);

        // --- Telegram Bot API Endpoint ---
        String apiUrl = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        // --- Prepare the JSON payload ---
        // Using String.format for simplicity, but a JSON library is better
        // for complex messages or proper escaping.
        String jsonBody = String.format("{\"chat_id\": \"%s\", \"text\": \"%s\"}", chatId, messageText);

        // --- Use HttpClient to send the POST request ---
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json") // Same header as curl
                .POST(BodyPublishers.ofString(jsonBody))   // Send the JSON body as POST data
                .build();

        LOGGER.info("Sending message to chat ID: " + chatId + " (from env var " + CHAT_ID_ENV_VAR + ")");
        LOGGER.info("Using bot token from env var " + BOT_TOKEN_ENV_VAR); // Avoid printing the token itself
        LOGGER.info("API URL: " + apiUrl.substring(0, apiUrl.indexOf("/bot") + 5) + "..."); // Print part of URL without full token
        LOGGER.info("JSON Body: " + jsonBody);

        try {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

            // --- Process the response ---
            int statusCode = response.statusCode();
            String responseBody = response.body();

            LOGGER.info("HTTP Status Code: " + statusCode);
            LOGGER.info("Response Body: " + responseBody);

            if (statusCode == 200) {
                LOGGER.info("Message sent successfully!");
            } else {
                LOGGER.error("Failed to send message. Telegram API returned an error.");
                // You might want to parse the responseBody to get the specific error message
            }

        } catch (IOException e) {
            LOGGER.error("Error sending HTTP request: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            LOGGER.error("Request interrupted: " + e.getMessage());
            e.printStackTrace();
            // Restore interrupted state
            Thread.currentThread().interrupt();
        }
    }
}
