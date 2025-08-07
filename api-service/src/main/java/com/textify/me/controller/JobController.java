package com.textify.me.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.textify.me.dto.JobStatusResponse;
import com.textify.me.dto.UploadResponse;
import com.textify.me.service.JobService;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

     private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        UploadResponse response = jobService.initiateFileUpload(file);
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    @GetMapping("/{jobId}/status")
    public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable String jobId) {
        // Delegate to JobService to fetch the job status
        JobStatusResponse response = jobService.getJobStatus(jobId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    @GetMapping("/{jobId}/download")
    public ResponseEntity<Void> downloadProcessedFile(@PathVariable String jobId) {
        // Delegate to JobService to get the GCS download URL
        String downloadUrl = jobService.getDownloadUrl(jobId);

        // Build the response with a 302 Found status and the Location header
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, downloadUrl);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
    @GetMapping("/recent")
    public ResponseEntity<List<JobStatusResponse>> getRecentJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        // Delegate to JobService to get the paginated list of jobs
        List<JobStatusResponse> recentJobs = jobService.getRecentJobs(page, size);
        return new ResponseEntity<>(recentJobs, HttpStatus.OK);
    }
    
}
