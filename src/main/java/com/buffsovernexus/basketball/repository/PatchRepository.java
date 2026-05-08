package com.buffsovernexus.basketball.repository;

import com.buffsovernexus.basketball.entity.Patch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PatchRepository extends JpaRepository<Patch, UUID> {
    boolean existsByVersion(String version);
}

