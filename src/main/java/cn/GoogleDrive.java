package cn;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.client.http.FileContent;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class GoogleDrive {

    private String application_name;
    private JsonFactory json_factory;

    public GoogleDrive() throws GeneralSecurityException, FileNotFoundException, IOException {
        application_name = "FCUL_Project";
        json_factory = JacksonFactory.getDefaultInstance();
    }

    public void GetShare(String share) {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            String secret = System.getenv("drive_secret");
            // Load client secrets
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(json_factory,
                    new InputStreamReader(new FileInputStream(secret)));

            // Set up authorization code flow
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, json_factory, clientSecrets, Collections.singleton(DriveScopes.DRIVE_FILE))
                    .setDataStoreFactory(new FileDataStoreFactory(new java.io.File("tokens")))
                    .setAccessType("offline")
                    .build();

            // Authorize
            Credential credential = new AuthorizationCodeInstalledApp(
                    flow, new LocalServerReceiver()).authorize("user");

            // Initialize the Drive service
            Drive service = new Drive.Builder(HTTP_TRANSPORT, json_factory, credential)
                    .setApplicationName(application_name)
                    .build();

            // Now you can use 'service' to interact with the Google Drive API

            // Check if the file exists
            FileList result = service.files().list()
                    .setPageSize(10)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
            List<File> files = result.getFiles();
            String fileId = null;
            for (File file : files) {
                if (file.getName().equals(share)) {
                    fileId = file.getId();
                    break;
                }
            }

            if (fileId != null) {
                //System.out.println("Share exists on Google Drive");

                // If the file exists, download it
                File file = service.files().get(fileId).execute();

                java.io.File fileOnDisk = new java.io.File("shares/share_googledrive.txt");

                try (OutputStream outputStream = new FileOutputStream(fileOnDisk)) {
                    // Export Google Docs files to 'text/plain' MIME type
                    if ("application/vnd.google-apps.document".equals(file.getMimeType())) {
                        String exportLink = file.getExportLinks().get("text/plain");
                        if (exportLink != null) {
                            service.getRequestFactory().buildGetRequest(new GenericUrl(exportLink))
                                    .execute().download(outputStream);
                        }
                    } else {
                        // For other file types, directly download content
                        service.files().get(fileId).executeMediaAndDownloadTo(outputStream);
                    }

                    System.out.println("Downloaded share from Google Drive");
                }
            }
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    public void UploadShare(String share) throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        // Load client secrets
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(json_factory,
                new InputStreamReader(new FileInputStream("client_secret_456844246425-fb44uenqorihdnse8ks8bm4n0o051j79.apps.googleusercontent.com.json")));

        // Set up authorization code flow
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, json_factory, clientSecrets, Collections.singleton(DriveScopes.DRIVE_FILE))
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File("tokens")))
                .setAccessType("offline")
                .build();

        // Authorize
        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("user");

        // Initialize the Drive service
        Drive service = new Drive.Builder(HTTP_TRANSPORT, json_factory, credential)
                .setApplicationName(application_name)
                .build();

        // Now you can use 'service' to interact with the Google Drive API

        // Check if the file exists
        FileList result = service.files().list()
                .setPageSize(10)
                .setFields("nextPageToken, files(id, name)")
                .execute();
        List<File> files = result.getFiles();
        String fileId = null;
        for (File file : files) {
            if (file.getName().equals(share)) {
                fileId = file.getId();
                break;
            }
        }

        if(fileId == null) {
            // If the file doesn't exist, upload it
            //System.out.println("Uploaded Share to GoogleDrive");
            // Specify the file you want to upload
            java.io.File fileContent = new java.io.File(share);

            // Set metadata for the file
            File fileMetadata = new File();
            fileMetadata.setName(share);
            fileMetadata.setMimeType("text/plain");

            // Create a FileContent instance with the file's MIME type and content
            FileContent mediaContent = new FileContent("text/plain", fileContent);

            // Use the Drive service to upload the file
            File uploadedFile = service.files().create(fileMetadata, mediaContent)
                    .setFields("id, name")
                    .execute();

            System.out.println("Share uploaded: " + uploadedFile.getName() + " (ID: " + uploadedFile.getId() + ")");
        } else {
            //System.out.println("Share exists in GoogleDrive");
        }
    }
    public void UploadFile(String thefile) throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        // Load client secrets
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(json_factory,
                new InputStreamReader(new FileInputStream("client_secret_456844246425-fb44uenqorihdnse8ks8bm4n0o051j79.apps.googleusercontent.com.json")));

        // Set up authorization code flow
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, json_factory, clientSecrets, Collections.singleton(DriveScopes.DRIVE_FILE))
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File("tokens")))
                .setAccessType("offline")
                .build();

        // Authorize
        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("user");

        // Initialize the Drive service
        Drive service = new Drive.Builder(HTTP_TRANSPORT, json_factory, credential)
                .setApplicationName(application_name)
                .build();

        // Now you can use 'service' to interact with the Google Drive API

        // Check if the file exists
        FileList result = service.files().list()
                .setPageSize(10)
                .setFields("nextPageToken, files(id, name)")
                .execute();
        List<File> files = result.getFiles();
        String fileId = null;
        for (File file : files) {
            if (file.getName().equals(thefile)) {
                fileId = file.getId();
                break;
            }
        }

        if(fileId == null) {
            // If the file doesn't exist, upload it
            System.out.println("Uploaded Share to GoogleDrive");
            // Specify the file you want to upload
            java.io.File fileContent = new java.io.File(thefile);

            // Set metadata for the file
            File fileMetadata = new File();
            fileMetadata.setName(thefile);
            fileMetadata.setMimeType("text/plain");

            // Create a FileContent instance with the file's MIME type and content
            FileContent mediaContent = new FileContent("text/plain", fileContent);

            // Use the Drive service to upload the file
            File uploadedFile = service.files().create(fileMetadata, mediaContent)
                    .setFields("id, name")
                    .execute();

            System.out.println("Share uploaded: " + uploadedFile.getName() + " (ID: " + uploadedFile.getId() + ")");
        } else {
            System.out.println("Share exists in GoogleDrive");
        }
    }

}
