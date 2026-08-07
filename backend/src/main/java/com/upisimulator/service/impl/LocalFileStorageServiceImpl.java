package com.upisimulator.service.impl;

import com.upisimulator.exception.InvalidRequestException;
import com.upisimulator.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Stores profile pictures on local disk under {@code app.upload.dir}, served
 * back out by {@link com.upisimulator.config.WebConfig}'s resource handler.
 * <p>
 * Filenames are always a fresh {@code UUID} generated here - the client's
 * original filename is never used for anything, including in error
 * messages, which rules out path traversal by construction rather than by
 * sanitizing untrusted input.
 */
@Service
@Slf4j
public class LocalFileStorageServiceImpl implements FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");
    private static final long MAX_FILE_SIZE_BYTES = 2L * 1024 * 1024;
    private static final String URL_PREFIX = "/uploads/profile-pictures/";

    private final Path uploadDirectory;

    public LocalFileStorageServiceImpl(@Value("${app.upload.dir}") String uploadDir) {
        this.uploadDirectory = Paths.get(uploadDir, "profile-pictures").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory: " + this.uploadDirectory, e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("No file was uploaded");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new InvalidRequestException("Only JPEG and PNG images are allowed");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidRequestException("Image must be 2MB or smaller");
        }

        String extension = "image/png".equals(file.getContentType()) ? ".png" : ".jpg";
        String filename = UUID.randomUUID() + extension;

        try {
            Path target = uploadDirectory.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store uploaded file", e);
        }

        return URL_PREFIX + filename;
    }

    @Override
    public void delete(String urlPath) {
        if (urlPath == null || urlPath.isBlank()) {
            return;
        }
        String filename = urlPath.substring(urlPath.lastIndexOf('/') + 1);
        try {
            Files.deleteIfExists(uploadDirectory.resolve(filename));
        } catch (IOException e) {
            log.warn("Could not delete old profile picture file: {}", urlPath, e);
        }
    }

}
