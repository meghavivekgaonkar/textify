package com.textify.worker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;

@Service
public class GCSService {

    private final Storage storage;
    private final String rawUploadsBucketName;
    private final String processedFilesBucketName;

    public GCSService(Storage storage,
                      @Value("${spring.cloud.gcp.storage.bucket-name.raw-uploads}") String rawUploadsBucketName,
                      @Value("${spring.cloud.gcp.storage.bucket-name.processed-files}") String processedFilesBucketName) {
        this.storage = storage;
        this.rawUploadsBucketName = rawUploadsBucketName;
        this.processedFilesBucketName = processedFilesBucketName;
    }

    public byte[] downloadFile(String gcsPath) {
        try {
            // Parse bucket and blob name from gcsPath (e.g., gs://bucket/blob)
            String pathWithoutPrefix = gcsPath.substring("gs://".length());
            int firstSlash = pathWithoutPrefix.indexOf('/');
            String bucket = pathWithoutPrefix.substring(0, firstSlash);
            String blobName = pathWithoutPrefix.substring(firstSlash + 1);

            BlobId blobId = BlobId.of(bucket, blobName);
            Blob blob = storage.get(blobId);
            if (blob == null) {
                throw new RuntimeException("File not found in GCS: " + gcsPath);
            }
            return blob.getContent();
        } catch (StorageException e) {
            throw new RuntimeException("Failed to download file from GCS: " + gcsPath, e);
        }
    }

    public String uploadFile(byte[] content, String bucketName, String gcsBlobName, String contentType) {
        try {
            BlobId blobId = BlobId.of(bucketName, gcsBlobName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                                        .setContentType(contentType)
                                        .build();
            storage.create(blobInfo, content);
            return String.format("gs://%s/%s", bucketName, gcsBlobName);
        } catch (StorageException e) {
            throw new RuntimeException("Failed to upload file to GCS: " + gcsBlobName, e);
        }
    }

    // Expose bucket names if needed by other services
    public String getRawUploadsBucketName() {
        return rawUploadsBucketName;
    }

    public String getProcessedFilesBucketName() {
        return processedFilesBucketName;
    }
}