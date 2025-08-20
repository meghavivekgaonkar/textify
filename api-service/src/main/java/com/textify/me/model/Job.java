package com.textify.me.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "processing_jobs")
@NoArgsConstructor
@Data
@AllArgsConstructor
public class Job {
	@Id
	String id;

	@Column
	String userId;
	
	@Column
	String original_filename;
	
	@Column
	String original_gcs_path;

	@Column
	String processed_gcs_path;
	
	@Column
	String status;
	
	@Column
	String fileType;
	
	@Column
	String mimeType;
	
	@Column
	String extracted_text_preview;
	
	@Column
	String error_message;
	
	@Column
	Instant createdAt;
	
	@Column
	Instant updatedAt;
	
}
