package com.textify.me.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.textify.me.model.Job;

public interface JobRepository extends JpaRepository<Job, String> {

    public Job findTopByUserIdOrderByCreatedAtDesc(String userId);
}

