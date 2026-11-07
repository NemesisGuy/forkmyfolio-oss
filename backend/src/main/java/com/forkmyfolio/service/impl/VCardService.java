package com.forkmyfolio.service.impl;

import com.forkmyfolio.model.PortfolioProfile;
import com.forkmyfolio.model.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VCardService {

    private static final Logger log = LoggerFactory.getLogger(VCardService.class);

    /**
     * Generates a vCard file from a user's portfolio profile.
     * This service method operates on the domain model provided by the controller.
     *
     * @param profile The fully populated PortfolioProfile of the user.
     * @return A record containing the vCard content and a suggested filename.
     */
    public VCardFile generateVCard(PortfolioProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("PortfolioProfile cannot be null when generating a vCard.");
        }
        User user = profile.getUser();
        if (user == null) {
            throw new IllegalStateException("PortfolioProfile must have an associated User to generate a vCard.");
        }

        String fullName = user.getFirstName() + " " + user.getLastName();

        StringBuilder vcfBuilder = new StringBuilder();
        vcfBuilder.append("BEGIN:VCARD\n");
        vcfBuilder.append("VERSION:3.0\n");
        vcfBuilder.append("FN:").append(fullName).append("\n");
        vcfBuilder.append("N:").append(user.getLastName()).append(";").append(user.getFirstName()).append(";;;\n");

        if (profile.getPublicEmail() != null && !profile.getPublicEmail().isBlank()) {
            vcfBuilder.append("EMAIL;TYPE=INTERNET:").append(profile.getPublicEmail()).append("\n");
        }
        if (profile.getWebsiteUrl() != null && !profile.getWebsiteUrl().isBlank()) {
            vcfBuilder.append("URL:").append(profile.getWebsiteUrl()).append("\n");
        }

        // Use the user's main profile image for the vCard photo.
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isBlank()) {
            getBase64ImageFromUrl(user.getProfileImageUrl()).ifPresent(base64Image -> {
                // Assuming JPEG format, which is common. A more advanced implementation
                // could inspect the image bytes or URL to determine the type.
                vcfBuilder.append("PHOTO;TYPE=JPEG;ENCODING=BASE64:").append(base64Image).append("\n");
                log.info("Successfully added photo to vCard for user: {}", user.getEmail());
            });
        }

        vcfBuilder.append("END:VCARD\n");

        String filename = String.format("%s%s.vcf", user.getFirstName(), user.getLastName());
        return new VCardFile(vcfBuilder.toString().getBytes(), filename);
    }

    /**
     * Fetches an image from a URL and encodes it as a Base64 string.
     *
     * @param imageUrl The URL of the image to fetch.
     * @return An Optional containing the Base64 encoded image string, or empty if fetching fails.
     */
    private Optional<String> getBase64ImageFromUrl(String imageUrl) {
        log.debug("Attempting to fetch and encode image for vCard from URL: {}", imageUrl);
        // Use a try-with-resources block for automatic resource management
        try (InputStream in = new URL(imageUrl).openStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096]; // A slightly larger buffer can be more efficient
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            byte[] imageBytes = out.toByteArray();
            return Optional.of(Base64.getEncoder().encodeToString(imageBytes));
        } catch (IOException e) {
            log.error("Failed to fetch or encode image for vCard from URL '{}': {}", imageUrl, e.getMessage());
            return Optional.empty();
        }
    }

    public record VCardFile(byte[] content, String suggestedFilename) {
    }
}