package com.textify.me.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor 
@AllArgsConstructor 
public class JobStatusResponse {

    private String jobId;
    private String status;
    private String originalFilename;
    private String errorMessage; // Nullable
    private Instant createdAt;
    private String downloadUrl;  // Nullable, only present if status is COMPLETED
}