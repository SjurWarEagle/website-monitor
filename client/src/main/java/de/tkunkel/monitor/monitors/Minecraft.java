package de.tkunkel.monitor.monitors; // Assuming a package structure

import de.tkunkel.monitor.starter.Starter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element; // Import Element
import org.jsoup.select.Elements; // Import Elements
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Version; // Import Version for clarity
import java.nio.charset.StandardCharsets; // Needed if processing body
import java.util.Collections;
import java.util.List; // Import List
import java.util.ArrayList; // Import ArrayList

@Service
public class Minecraft extends Monitor {
    private final TelegramMessageSender telegramMessageSender;
    private static final Logger LOGGER = LoggerFactory.getLogger(Minecraft.class);

    private static final String URL =
            "https://www.minecraft.net/en-us/download/server/bedrock";

    // User-Agent from the image
    private static final String BROWSER_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36";

    public Minecraft(TelegramMessageSender telegramMessageSender) {
        this.telegramMessageSender = telegramMessageSender;
    }


    public static void main(String[] args) {
        var serverEntries = new Minecraft(new TelegramMessageSender()).collectServerEntries();
        if (serverEntries.isEmpty()) {
            LOGGER.info("No changelog entries found containing 'Bedrock' in the text.");
            // Optional: Inspect the HTML structure manually or print sample link text
            // LOGGER.info("Sample link texts encountered:");
            // potentialChangelogLinks.stream().limit(10).forEach(el -> LOGGER.info("  " + el.text()));
        } else {
            LOGGER.info("Found " + serverEntries.size() + " Bedrock changelog entries:");
            for (String entry : serverEntries) {
                LOGGER.info(entry);
            }
        }

    }

    public List<String> collectServerEntries() {
        LOGGER.info("Attempting to download URL using HTTP/2: " + URL);

        // Create an HttpClient that *must* use HTTP/2
        HttpClient httpClient = HttpClient.newBuilder()
                .version(Version.HTTP_2) // Explicitly request HTTP/2
                // Optional: configure timeouts, follow redirects, etc.
                // .connectTimeout(java.time.Duration.ofSeconds(10))
                // .followRedirects(HttpClient.Redirect.NORMAL) // Default is NEVER
                .build();

        // Build the HTTP GET request with headers from the image
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))

                // -----------------------------------------------------------------------------
                // Using headers from the image that are generally safe and allowed by java.net.http.HttpClient
                .header("User-Agent", BROWSER_USER_AGENT)
                .header("Referer", "https://feedback.minecraft.net/hc/en-us/sections/360001186971-Release-Changelogs")
                .header("X-Requested-With", "XMLHttpRequest") // Indicates an XHR request
                // -----------------------------------------------------------------------------

                .GET() // Specify GET method (default)
                .build();

        try {
            // Send the request and get the response body as a String
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            // Check the status code
            int statusCode = response.statusCode();
            LOGGER.info("Response Status Code: " + statusCode);

            // --- VERIFY PROTOCOL ---
            Version negotiatedVersion = response.version();
            LOGGER.info("Negotiated Protocol Version: " + negotiatedVersion);

            if (negotiatedVersion != Version.HTTP_2) {
                System.err.println(
                        "ERROR: Did NOT use HTTP/2 as required. Negotiated: " +
                                negotiatedVersion
                );
                // Exit or throw if strictly requiring HTTP/2
                System.exit(1);
            } else {
                LOGGER.info("SUCCESS: Used HTTP/2 as required.");
            }
            // -----------------------


            return parseResponse(statusCode, response);

        } catch (IOException e) {
            System.err.println(
                    "An I/O error occurred during the HTTP/2 request (or HTTP/2 negotiation failed):"
            );
            LOGGER.error("Error: " + e);
        } catch (InterruptedException e) {
            System.err.println("The HTTP request was interrupted:");
            LOGGER.error("Error: " + e);
            // Restore interrupted state
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("An unexpected error occurred:");
            LOGGER.error("Error: " + e);
        }
        return Collections.emptyList();
    }

    private static List<String> parseResponse(int statusCode, HttpResponse<String> response) {
        List<String> serverZipEntires = new ArrayList<>();
        if (statusCode == 200) {
            LOGGER.info("Successfully downloaded content via HTTP/2 (Status 200 OK).");
            // --- Jsoup parsing and filtering for Bedrock entries ---
            try {
                Document doc = Jsoup.parse(response.body(), URL); // Parse with base URL

                // Select elements that likely contain the changelog titles.
                // On ZenDesk/Help Center sites, these are often links.
                // A common selector for article links in a list is "a.article-list-link"
                // or simply targeting links within the main content area.
                // Let's start by selecting all links and checking their text content.
                Elements potentialChangelogLinks = doc.select("a");


                LOGGER.info("Searching for 'Bedrock' changelog entries...");

                for (Element linkElement : potentialChangelogLinks) {
                    String linkText = linkElement.text(); // Get the visible text of the link
                    String linkUrl = linkElement.absUrl("href"); // Get the absolute URL of the link

                    // Check if the text contains "Bedrock" (case-insensitive)
                    if (linkUrl.toLowerCase().contains("bedrock")
                            && linkUrl.toLowerCase().contains("linux")
                            && !linkUrl.toLowerCase().contains("preview")
                            && linkUrl.toLowerCase().contains(".zip")
                    ) {
                        serverZipEntires.add("- " + linkText + " (" + linkUrl + ")");
                    }
                }

            } catch (Exception e) {
                System.err.println("Error parsing HTML or selecting elements with Jsoup: " + e.getMessage());
                LOGGER.error("Error: " + e);
            }
            // ----------------------------------------------------
            return serverZipEntires;
        } else {
            System.err.println("Failed to download content. Server returned non-200 status.");
            // Print response body for non-200 codes
            System.err.println("Response Body:\n" + response.body());
        }
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "Minecraft Current Bedrock Server";
    }

    @Override
    public String getConfigFileName() {
        return "minecraft.dat";
    }

    @Override
    public void execute() {
        List<String> strings = collectServerEntries();
        String newValue = strings.get(0).trim();
        String oldValue = readOldValue().trim();
        LOGGER.debug("Old: " + oldValue);
        LOGGER.debug("New: " + newValue);

        if (!oldValue.equalsIgnoreCase(newValue)) {
            storeNewValue(newValue);
            String msg = ("⚒ Minecraft Bedrock Server Change detected!\nOld: '" + oldValue + "',\n New: '" + newValue + "'");
            telegramMessageSender.sendMessage(msg);
        }
    }
}
