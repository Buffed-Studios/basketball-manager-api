package com.buffsovernexus.basketball.controller;

import com.buffsovernexus.basketball.dto.CreatePatchRequest;
import com.buffsovernexus.basketball.dto.PatchResponse;
import com.buffsovernexus.basketball.service.PatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patches")
@RequiredArgsConstructor
public class PatchController {

    private final PatchService patchService;

    /**
     * GET /api/patches
     * Public — retrieves all patch notes ordered by newest first.
     * No login required; JWT is accepted but not enforced.
     */
    @GetMapping
    public ResponseEntity<List<PatchResponse>> getAll() {
        return ResponseEntity.ok(patchService.getAllPatches());
    }

    /**
     * POST /api/patches
     * Requires the 'admin.create.patch' access node OR superuser status.
     */
    @PreAuthorize("hasAuthority('admin.create.patch') or hasRole('SUPERUSER')")
    @PostMapping
    public ResponseEntity<PatchResponse> create(@Valid @RequestBody CreatePatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patchService.createPatch(request));
    }
}

