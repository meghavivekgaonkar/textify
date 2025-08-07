package com.textify.worker.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.textify.worker.model.Job;

public interface JobRepository extends JpaRepository<Job, String> {
}
