package com.textify.me.api_service;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import com.textify.me.controller.JobController;
import com.textify.me.service.JobService;

class ApiServiceApplicationTests {

    private JobService jobService;
    private JobController jobController;

    @BeforeEach
    void setUp() {
        jobService = Mockito.mock(JobService.class);
        jobController = new JobController(jobService);
    }

    // @Test
    // void testUploadFileCreatesJob() throws Exception {
    //     MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "dummy".getBytes());
    //     Job job = new Job();
    //     job.setId(1L);
    //     job.setStatus("PENDING");
    //     when(jobService.createJob(any(), anyString())).thenReturn(job);

    //     ResponseEntity<?> response = jobController.uploadFile(file, "image/png");
    //     assertEquals(200, response.getStatusCodeValue());
    //     assertTrue(response.getBody().toString().contains("PENDING"));
    // }

    // @Test
    // void testGetJobStatusReturnsJob() {
    //     Job job = new Job();
    //     job.setId(2L);
    //     job.setStatus("COMPLETED");
    //     when(jobService.getJobById(2L)).thenReturn(Optional.of(job));

    //     ResponseEntity<?> response = jobController.getJobStatus(2L);
    //     assertEquals(200, response.getStatusCodeValue());
    //     assertTrue(response.getBody().toString().contains("COMPLETED"));
    // }

    // @Test
    // void testGetJobStatusNotFound() {
    //     when(jobService.getJobById(99L)).thenReturn(Optional.empty());

    //     ResponseEntity<?> response = jobController.getJobStatus(99L);
    //     assertEquals(404, response.getStatusCodeValue());
    // }

    // @Test
    // void testDownloadProcessedFileRedirect() {
    //     Job job = new Job();
    //     job.setId(3L);
    //     job.setStatus("COMPLETED");
    //     job.setProcessedFileUrl("https://storage.googleapis.com/bucket/processed.pdf");
    //     when(jobService.getJobById(3L)).thenReturn(Optional.of(job));

    //     ResponseEntity<?> response = jobController.downloadProcessedFile(3L);
    //     assertEquals(302, response.getStatusCodeValue());
    //     assertEquals("https://storage.googleapis.com/bucket/processed.pdf", response.getHeaders().getLocation().toString());
    // }

    // @Test
    // void testRecentJobsPagination() {
    //     Job job1 = new Job(); job1.setId(1L);
    //     Job job2 = new Job(); job2.setId(2L);
    //     List<Job> jobs = Arrays.asList(job1, job2);
    //     when(jobService.getRecentJobs(anyInt(), anyInt())).thenReturn(jobs);

    //     ResponseEntity<?> response = jobController.getRecentJobs(0, 2);
    //     assertEquals(200, response.getStatusCodeValue());
    //     assertTrue(response.getBody().toString().contains("1"));
    //     assertTrue(response.getBody().toString().contains("2"));
    // }
}