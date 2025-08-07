package com.textify.worker.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.textify.worker.model.Job;
import com.textify.worker.repository.JobRepository;

import jakarta.transaction.Transactional;

@Service
public class WorkerProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(WorkerProcessingService.class);
    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_BACKOFF_MILLIS = 1000; // 1 second

    private final JobRepository jobRepository;
    private final GCSService gcsService;
    private final TesseractOcrService tesseractOcrService;

    @Value("${spring.cloud.gcp.storage.bucket-name.processed-files}")
    private String processedFilesBucketName;

    public WorkerProcessingService(JobRepository jobRepository, GCSService gcsService, TesseractOcrService tesseractOcrService) {
        this.jobRepository = jobRepository;
        this.gcsService = gcsService;
        this.tesseractOcrService = tesseractOcrService;
    }

    /**
     * Processes a job, with retry logic in case the job is not found immediately.
     * This handles the race condition where the Pub/Sub message arrives before
     * the database transaction has committed.
     *
     * @param jobId The ID of the job to process.
     */
    public void processJobWithRetry(String jobId) {
        int retryCount = 0;
        long backoff = INITIAL_BACKOFF_MILLIS;

        while (retryCount < MAX_RETRIES) {
            try {
                Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found in DB: " + jobId));

                // If the job is found, proceed with processing
                logger.info("Processing job: {}", job.getId());
                _doProcessJob(job, job.getOriginal_gcs_path());
                logger.info("Successfully processed job: {}", job.getId());
                return; // Exit the loop on success
            } catch (RuntimeException e) {
                // The job was not found, so we'll retry.
                retryCount++;
                logger.warn("Job {} not found on attempt {}. Retrying in {}ms...", jobId, retryCount, backoff);

                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("Retry thread interrupted", ie);
                    break;
                }

                // Exponential backoff
                backoff *= 2;
            }
        }

        // If the loop finishes without success, log a fatal error.
        logger.error("Job {} failed after {} retries. Giving up.", jobId, MAX_RETRIES);
    }

    @Transactional
    private void _doProcessJob(Job job, String originalGcsPath) {
        String errorMessage = null;
        try {
            // 1. Update Status to PROCESSING
            job.setStatus("PROCESSING");
            job.setUpdatedAt(Instant.now());
            jobRepository.save(job);
            logger.info("Job {} status updated to PROCESSING.", job.getId());

            // 2. Download Original File from GCS
            byte[] fileBytes = gcsService.downloadFile(originalGcsPath);
            logger.info("File for job {} downloaded from GCS. Size: {} bytes", job.getId(), fileBytes.length);

            // 3. Extract Text (OCR or PDF parsing)
            String extractedText;
            String fileTypeCategory = job.getFileType();
            String originalFilename = job.getOriginal_filename();

            if ("image".equals(fileTypeCategory)) {
                extractedText = tesseractOcrService.extractTextFromImage(fileBytes);
            } else if ("pdf".equals(fileTypeCategory)) {
                extractedText = tesseractOcrService.extractTextFromPdf(fileBytes);
            } else {
                throw new UnsupportedOperationException("Unsupported file type for processing: " + fileTypeCategory);
            }

            logger.info("Text extracted for job {}. Extracted text length: {}", job.getId(), extractedText.length());

            // 4. Upload Processed Text to GCS
            String processedGcsBlobName = job.getId() + "/" + getBaseFileName(originalFilename) + ".txt"; // Save as .txt

            String processedGcsPath = gcsService.uploadFile(
                extractedText.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                this.processedFilesBucketName,
                processedGcsBlobName,
                "text/plain"
            );
            logger.info("Processed text for job {} uploaded to GCS at {}.", job.getId(), processedGcsPath);

            // 5. Update Job Status to COMPLETED
            job.setStatus("COMPLETED");
            job.setProcessed_gcs_path(processedGcsPath);
            job.setUpdatedAt(Instant.now());
            jobRepository.save(job);
            logger.info("Job {} status updated to COMPLETED.", job.getId());

        } catch (Exception e) {
            errorMessage = "Processing failed: " + e.getMessage();
            logger.error("Job {} failed: {}", job.getId(), errorMessage, e);
            if (job != null) {
                job.setStatus("FAILED");
                job.setError_message(errorMessage.substring(0, Math.min(errorMessage.length(), 255)));
                job.setUpdatedAt(Instant.now());
                jobRepository.save(job);
                logger.info("Job {} status updated to FAILED.", job.getId());
            }
            throw new RuntimeException("Job processing failed for " + job.getId(), e);
        }
    }

    private String getBaseFileName(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? filename : filename.substring(0, dotIndex);
    }
}