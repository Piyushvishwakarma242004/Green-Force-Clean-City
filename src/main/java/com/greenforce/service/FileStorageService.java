package com.greenforce.service;

import com.greenforce.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/** Saves uploaded issue photos to disk under a random name (the client's file name is never used). */
@Service
public class FileStorageService {

    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp");

    private final Path root;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create upload directory " + root, e);
        }
    }

    public Path getRoot() {
        return root;
    }

    /** @return the stored file name (not the full path) */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image is required");
        }
        String extension = ALLOWED_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new BadRequestException("Only JPG, PNG, GIF or WEBP images are allowed");
        }
        String storedName = UUID.randomUUID() + extension;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, root.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store image", e);
        }
        return storedName;
    }

    public void deleteQuietly(String storedName) {
        if (storedName == null || storedName.contains("/") || storedName.contains("\\") || storedName.contains("..")) {
            return;
        }
        try {
            Files.deleteIfExists(root.resolve(storedName));
        } catch (IOException ignored) {
            // nothing useful to do
        }
    }
}
