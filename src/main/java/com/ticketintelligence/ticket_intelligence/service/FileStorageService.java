package com.ticketintelligence.ticket_intelligence.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDirectory =
            Paths.get("uploads");

    public FileStorageService()
            throws IOException {

        Files.createDirectories(
                uploadDirectory
        );
    }

    public String saveFile(
            MultipartFile file,
            String folder)
            throws IOException {

        if (file == null ||
                file.isEmpty()) {

            throw new IllegalArgumentException(
                    "File is empty."
            );
        }

        if (file.getSize() >
                5 * 1024 * 1024) {

            throw new IllegalArgumentException(
                    "File size cannot exceed 5 MB."
            );
        }

        String contentType =
                file.getContentType();

        if (contentType == null ||
                !isAllowedImage(contentType)) {

            throw new IllegalArgumentException(
                    "Only JPG, JPEG, PNG and WEBP images are allowed."
            );
        }

        Path folderPath =
                uploadDirectory.resolve(folder);

        Files.createDirectories(
                folderPath
        );

        String originalName =
                file.getOriginalFilename();

        String extension = "";

        if (originalName != null &&
                originalName.contains(".")) {

            extension =
                    originalName.substring(
                            originalName.lastIndexOf(".")
                    );
        }

        String fileName =
                UUID.randomUUID()
                        .toString()
                        + extension;

        Path destination =
                folderPath.resolve(fileName)
                        .normalize();

        if (!destination.startsWith(
                folderPath.normalize())) {

            throw new IllegalArgumentException(
                    "Invalid file name."
            );
        }

        Files.copy(
                file.getInputStream(),
                destination,
                StandardCopyOption.REPLACE_EXISTING
        );

        return "/uploads/" +
                folder +
                "/" +
                fileName;
    }

    private boolean isAllowedImage(
            String contentType) {

        return contentType.equals(
                "image/jpeg"
        )
                ||
                contentType.equals(
                        "image/png"
                )
                ||
                contentType.equals(
                        "image/webp"
                );
    }
}