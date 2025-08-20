package com.textify.me.service;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.textify.me.dto.JobStatusResponse;
import com.textify.me.dto.UploadResponse;
import com.textify.me.exception.InvalidFileException;
import com.textify.me.exception.JobNotFoundException;
import com.textify.me.model.Job;
import com.textify.me.repository.JobRepository;


@Service
public class JobService {

    private final JobRepository jobRepository;
	private final GCSService gcsService;
	private final PubSubPublisherService pubSubPublisherService;

	public JobService(JobRepository jobRepository, GCSService gcsService,
			PubSubPublisherService pubSubPublisherService) {
		this.jobRepository = jobRepository;
		this.gcsService = gcsService;
		this.pubSubPublisherService = pubSubPublisherService;
	}
    // Allowed MIME types]
	 private static final Map<String, String> FILE_EXTENSION_TO_CATEGORY_MAP;

    // Static initializer to populate the map at class loading time.
    static {
        Map<String, String> aMap = new HashMap<>();

        // Documents (originally tied to application/pdf)
        aMap.put("application/pdf", "pdf");

        // Images (originally tied to various image MIME types)
        aMap.put("image/jpg", "image");
        aMap.put("image/jpeg", "image");
        aMap.put("image/png", "image");
        aMap.put("image/gif", "image");
        aMap.put("image/bmp", "image");
        aMap.put("image/webp", "image");

        // Any extension not in this map is considered an unsupported file type.
        FILE_EXTENSION_TO_CATEGORY_MAP = Collections.unmodifiableMap(aMap);
    }

	@Transactional
	public UploadResponse initiateFileUpload(MultipartFile file, String userId) {

		// --- 1. Basic File Validation ---
		if (file.isEmpty()) {
			throw new InvalidFileException("Uploaded file is empty.");
		}
		String mimeType = file.getContentType();
		if (mimeType == null || !FILE_EXTENSION_TO_CATEGORY_MAP.containsKey(mimeType)) {
			throw new InvalidFileException("Unsupported file type: " + (mimeType != null ? mimeType : "unknown")
					+ ". Only images (JPEG, PNG, GIF, BMP, WebP) and PDFs are allowed.");
		}
		// --- 2. Generate Job ID ---
		String jobId = UUID.randomUUID().toString();
		String originalFilename = file.getOriginalFilename(); // Get original file name
		String fileExtension = getFileExtension(originalFilename); // Helper method to get extension
		String baseFileName = getBaseFileName(originalFilename); // Helper method to get base name

		// --- 3. Upload Raw File to GCS ---
		String gcsBlobName = jobId + "/" + baseFileName + "." + fileExtension; // e.g., UUID/my_document.pdf
		String originalGcsPath = gcsService.uploadFile(file, gcsBlobName);

		// --- 4. Persist Job Metadata to Cloud SQL ---
		Job job = new Job();
		job.setId(jobId);
		job.setUserId(userId); // Associate job with user
		job.setOriginal_filename(originalFilename);
		job.setOriginal_gcs_path(originalGcsPath);
		job.setStatus("UPLOADED"); // Initial status
		job.setFileType(FILE_EXTENSION_TO_CATEGORY_MAP.get(mimeType)); // 'image' or 'pdf'
		job.setMimeType(mimeType);
		job.setCreatedAt(Instant.now());
		job.setUpdatedAt(Instant.now());
		// processedGcsPath, errorMessage will be null initially

		jobRepository.save(job);

		// --- 5. Publish Message to Pub/Sub ---
		// The worker service will consume this message to start processing
		pubSubPublisherService.publishProcessingRequest(jobId, originalGcsPath, userId);

		return new UploadResponse(jobId, "UPLOADED", "File received and processing initiated.");
	}

	// Helper to extract file extension (e.g., "pdf" from "document.pdf")
	private String getFileExtension(String filename) {
		int dotIndex = filename.lastIndexOf('.');
		return (dotIndex == -1) ? "" : filename.substring(dotIndex + 1);
	}

	// Helper to extract base file name (e.g., "document" from "document.pdf")
	private String getBaseFileName(String filename) {
		int dotIndex = filename.lastIndexOf('.');
		return (dotIndex == -1) ? filename : filename.substring(0, dotIndex);
	}

	@Transactional(readOnly = true)
	public JobStatusResponse getJobStatus(String jobId) {
		Job job = jobRepository.findById(jobId)
				.orElseThrow(() -> new JobNotFoundException("Job with ID " + jobId + " not found."));

		// Map job entity to DTO and return
		return mapJobToJobStatusResponse(job);
	}

	private JobStatusResponse mapJobToJobStatusResponse(Job job) {
		return new JobStatusResponse(job.getId(),job.getUserId(), job.getStatus(), job.getOriginal_filename(), job.getError_message(),
				job.getCreatedAt(),
				// Only provide download URL if job is completed and path exists
				job.getStatus().equals("COMPLETED") && job.getProcessed_gcs_path() != null
						? gcsService.getPublicDownloadUrl(job.getProcessed_gcs_path())
						: null);
	}
	
	@Transactional(readOnly = true)
    public List<JobStatusResponse> getRecentJobs(int page, int size) {
        // Create a Pageable object for pagination and sorting by creation date (descending)
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Fetch a page of jobs from the repository
        Page<Job> jobPage = jobRepository.findAll(pageable);

        // Map the Job entities to DTOs
        return jobPage.getContent().stream()
                .map(this::mapJobToJobStatusResponse) // Reuse the mapping helper
                .collect(Collectors.toList());
    }
	@Transactional(readOnly = true)
    public UploadResponse getJobStatusByUserId(String userId) {
		
		Job job = jobRepository.findTopByUserIdOrderByCreatedAtDesc(userId);	
		UploadResponse response = new UploadResponse(job.getId(), job.getStatus(), "Job status retrieved successfully.");
		return response;
	}
	@Transactional(readOnly = true)
	public String getDownloadUrl(String jobId) {
		Job job = jobRepository.findById(jobId)
				.orElseThrow(() -> new JobNotFoundException("Job with ID " + jobId + " not found."));

		if (!"COMPLETED".equals(job.getStatus()) || job.getProcessed_gcs_path() == null) {
			// Throw a more specific exception if the file isn't ready
			throw new InvalidFileException(
					"Job with ID " + jobId + " is not yet completed or processed file is not available.");
		}

		// Use GcsService to generate the public download URL
		return gcsService.getPublicDownloadUrl(job.getProcessed_gcs_path());
	}

}