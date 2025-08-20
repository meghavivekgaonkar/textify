package com.textify.worker.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.textify.worker.model.Job;
// import com.textify.worker.model.JobOutbox;
import com.textify.worker.repository.JobOutboxRepository;
import com.textify.worker.repository.JobRepository;

public class JobInitiatorService {

    private final JobRepository jobRepository;
    private final JobOutboxRepository jobOutboxRepository;

    public JobInitiatorService(JobRepository jobRepository, JobOutboxRepository jobOutboxRepository) {
        this.jobRepository = jobRepository;
        this.jobOutboxRepository = jobOutboxRepository;
    }

    /**
     * Creates a new job and an outbox message in a single database transaction.
     * The Pub/Sub message will be sent by a separate service after the transaction commits.
     *
     * @param originalGcsPath The GCS path of the file to be processed.
     * @return The newly created Job object.
     */
    @Transactional
    public Job createNewJob(String originalGcsPath) {
        // 1. Create and save the new Job to the database
        Job newJob = new Job();
        newJob.setOriginal_gcs_path(originalGcsPath);

        // **FIX**: Determine the file type from the GCS path and set it.
        String fileType = getFileTypeFromGcsPath(originalGcsPath);
        newJob.setFileType(fileType);

        Job savedJob = jobRepository.save(newJob);

        // 2. Create and save a new message to the outbox table
        JobOutbox outboxMessage = new JobOutbox();
        outboxMessage.setJobId(savedJob.getId());
        outboxMessage.setUserId(savedJob.getUserId()); 
        outboxMessage.setPayload("{\"jobId\":\"" + savedJob.getId() + "\"}");
        jobOutboxRepository.save(outboxMessage);

        // The transaction commits here. Only after this point will the
        // outbox message be visible to the OutboxRelayService, which
        // then publishes it to Pub/Sub.
        return savedJob;
    }

    /**
     * Helper method to determine the file type based on the GCS path's file extension.
     * @param gcsPath The GCS path string.
     * @return The determined file type ("image", "pdf", or "unknown").
     */
    private String getFileTypeFromGcsPath(String gcsPath) {
        String filename = gcsPath.substring(gcsPath.lastIndexOf('/') + 1);
        String fileExtension = getFileExtension(filename).toLowerCase();

        if (fileExtension.equals("pdf")) {
            return "pdf";
        } else if (fileExtension.equals("jpg") || fileExtension.equals("jpeg") || fileExtension.equals("png") || fileExtension.equals("gif")) {
            return "image";
        }
        return "unknown"; // Default to unknown if not a recognized type
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex + 1);
    }
}