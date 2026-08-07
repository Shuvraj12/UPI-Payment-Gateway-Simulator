package com.upisimulator.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * Validates and stores the file, returning a URL path (e.g.
     * {@code /uploads/profile-pictures/<uuid>.jpg}) the frontend can use
     * directly as an {@code <img src>}.
     */
    String store(MultipartFile file);

    /**
     * Deletes a previously stored file, given the URL path {@link #store}
     * returned. Safe to call with {@code null} or blank.
     */
    void delete(String urlPath);

}
