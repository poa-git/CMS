package com.system.complaints.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleDriveService {

    @Value("${google.drive.folder-id}")
    private String folderId;

    @Value("${google.drive.credentials-json}")
    private String credentialsJson;

    private Drive driveService;

    @PostConstruct
    public void init() throws Exception {
        String credentialsJson = System.getenv("GOOGLE_CREDENTIALS_JSON");
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new ByteArrayInputStream(credentialsJson.getBytes()))
                .createScoped(Collections.singleton(DriveScopes.DRIVE));

        driveService = new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials)
        ).setApplicationName("Complaints System").build();
    }

    public String uploadFile(MultipartFile file) {
        try {
            // File metadata
            File fileMetadata = new File();
            fileMetadata.setName(file.getOriginalFilename());
            fileMetadata.setParents(List.of(folderId));

            // File content
            com.google.api.client.http.InputStreamContent mediaContent =
                    new com.google.api.client.http.InputStreamContent(
                            file.getContentType(),
                            new ByteArrayInputStream(file.getBytes())
                    );

            // Upload
            File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id, webViewLink, webContentLink")
                    .execute();

            // Make the file publicly viewable
            Permission permission = new Permission()
                    .setType("anyone")
                    .setRole("reader");
            driveService.permissions().create(uploadedFile.getId(), permission).execute();

            // Return a direct-view URL
            return "https://drive.google.com/uc?export=view&id=" + uploadedFile.getId();

        } catch (IOException e) {
            throw new RuntimeException("Google Drive upload failed", e);
        }
    }
}