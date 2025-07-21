package com.textify.me.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
    public String getPublicDownloadUrl(String gcsBlobPath) {
		// Parse the GCS path (e.g., "gs://your-bucket/path/to/file.txt")
		if (!gcsBlobPath.startsWith("gs://")) {
			throw new IllegalArgumentException("Invalid GCS path format: " + gcsBlobPath);
		}
		String pathWithoutPrefix = gcsBlobPath.substring("gs://".length());
		int firstSlash = pathWithoutPrefix.indexOf('/');
		if (firstSlash == -1) {
			throw new IllegalArgumentException("Invalid GCS path format (no blob name): " + gcsBlobPath);
		}
		String bucketName = pathWithoutPrefix.substring(0, firstSlash);
		String blobName = pathWithoutPrefix.substring(firstSlash + 1);

		// Construct the public URL format for GCS
		// Using "storage.googleapis.com" directly is for public objects
		// For project-specific access, typically
		// "https://storage.cloud.google.com/your-bucket/your-object"
		// Ensure blobName is URL-encoded for special characters
		try {
			String encodedBlobName = URLEncoder.encode(blobName, StandardCharsets.UTF_8.toString()).replace("+", "%20");
			return String.format("https://storage.googleapis.com/%s/%s", bucketName, encodedBlobName);
		} catch (Exception e) {
			throw new RuntimeException("Failed to encode GCS blob name: " + blobName, e);
		}
	}
}
