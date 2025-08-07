package com.textify.worker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.textify.worker.model.JobOutbox;

@Repository
public interface JobOutboxRepository extends JpaRepository<JobOutbox, String> {
}

