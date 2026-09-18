package com.ticketintelligence.ticket_intelligence.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductImageController {

    private static final Path UPLOAD_DIR =
            Paths.get("uploads", "products").toAbsolutePath().normalize();

    @PostMapping(
            value = "/upload-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> uploadProductImage(
            @RequestParam("image") MultipartFile image,
            Authentication authentication
    ) {

        try {

            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Please login first."));
            }

            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Please select an image."));
            }

            // 5 MB limit
            if (image.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Image must be smaller than 5 MB."));
            }

            String contentType = image.getContentType();

            if (contentType == null ||
                    !(contentType.equalsIgnoreCase("image/jpeg")
                            || contentType.equalsIgnoreCase("image/png")
                            || contentType.equalsIgnoreCase("image/webp"))) {

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "message",
                                "Only JPG, PNG and WEBP images are supported."
                        ));
            }

            Files.createDirectories(UPLOAD_DIR);

            String extension = getExtension(contentType);

            String fileName =
                    UUID.randomUUID().toString().replace("-", "") + extension;

            Path target = UPLOAD_DIR.resolve(fileName).normalize();

            // Prevent path traversal
            if (!target.getParent().equals(UPLOAD_DIR)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Invalid file."));
            }

            try (InputStream inputStream = image.getInputStream()) {
                Files.copy(
                        inputStream,
                        target,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            String imageUrl = "/uploads/products/" + fileName;

            return ResponseEntity.ok(
                    Map.of(
                            "message", "Image uploaded successfully",
                            "imageUrl", imageUrl
                    )
            );

        } catch (IOException e) {

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message",
                            "Could not save image."
                    ));
        }
    }

    private String getExtension(String contentType) {

        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}