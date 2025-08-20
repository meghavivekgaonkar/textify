package com.textify.worker.model;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "job_outbox")
@NoArgsConstructor
@Data
@AllArgsConstructor
public class JobOutbox {

    @Id
    private String jobId;

    private String userId;

    @Lob // Use @Lob for large text fields, such as JSON payloads
    private String payload;

    private Instant createdAt = Instant.now();

}
