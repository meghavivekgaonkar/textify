package com.textify.me.service;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;

@Service
public class GCSService {
    private final Storage storage; // Google Cloud Storage client
	private final String rawUploadsBucketName;
	private final String processedFilesBucketName;
	private final String gcpProjectId; // Injected for constructing public URLs
    public GCSService(Storage storage,
			@Value("${spring.cloud.gcp.storage.bucket-name.raw-uploads}") String rawUploadsBucketName,
			@Value("${spring.cloud.gcp.storage.bucket-name.processed-files}") String processedFilesBucketName,
			@Value("${spring.cloud.gcp.project-id}") String gcpProjectId) {
		this.storage = storage;
		this.rawUploadsBucketName = rawUploadsBucketName;
		this.processedFilesBucketName = processedFilesBucketName;
		this.gcpProjectId = gcpProjectId;
	}
    
    public String uploadFile(MultipartFile file, String gcsBlobName) {
		try {
			// Define the Blob (object) in GCS
			BlobId blobId = BlobId.of(rawUploadsBucketName, gcsBlobName);
			BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(file.getContentType()) // Set the MIME type
					.build();

			// Upload the file's bytes to GCS
			storage.create(blobInfo, file.getBytes());

			// Return the GCS URI (gs://bucket/blob)
			return String.format("gs://%s/%s", rawUploadsBucketName, gcsBlobName);

		} catch (IOException e) {
			// Handle IO exceptions during file reading
			throw new RuntimeException("Failed to read file for upload: " + file.getOriginalFilename(), e);
		} catch (StorageException e) {
			// Handle GCS specific exceptions (e.g., permissions, bucket not found)
			throw new RuntimeException("Failed to upload file to GCS: " + file.getOriginalFilename(), e);
		}
	}
   public String getPublicDownloadUrl(String gcsPath) {
        // Parse the GCS path (e.g., "gs://your-bucket/path/to/file.txt")
        if (!gcsPath.startsWith("gs://")) {
            throw new IllegalArgumentException("Invalid GCS path format: " + gcsPath);
        }
        String pathWithoutPrefix = gcsPath.substring("gs://".length());
        int firstSlash = pathWithoutPrefix.indexOf('/');
        if (firstSlash == -1) {
            throw new IllegalArgumentException("Invalid GCS path format (no blob name): " + gcsPath);
        }
        String bucketName = pathWithoutPrefix.substring(0, firstSlash);
        String objectName = pathWithoutPrefix.substring(firstSlash + 1);

        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName).build();

        // The signed URL will be valid for 15 minutes.
        // You can adjust this duration as needed.
        URL signedUrl = storage.signUrl(blobInfo, 15, TimeUnit.MINUTES, Storage.SignUrlOption.withV4Signature());

        return signedUrl.toString();
    }
}
