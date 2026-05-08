package com.buffsovernexus.basketball.service;

import com.buffsovernexus.basketball.dto.CreatePatchRequest;
import com.buffsovernexus.basketball.dto.PatchResponse;
import com.buffsovernexus.basketball.entity.Patch;
import com.buffsovernexus.basketball.repository.PatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatchService {

    private final PatchRepository patchRepository;

    @Transactional
    public PatchResponse createPatch(CreatePatchRequest request) {
        if (patchRepository.existsByVersion(request.version())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A patch with version '" + request.version() + "' already exists."
            );
        }

        Patch patch = patchRepository.save(
                Patch.builder()
                        .title(request.title())
                        .version(request.version())
                        .notes(request.notes())
                        .build()
        );

        return toResponse(patch);
    }

    @Transactional(readOnly = true)
    public List<PatchResponse> getAllPatches() {
        return patchRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toResponse)
                .toList();
    }

    private PatchResponse toResponse(Patch patch) {
        return new PatchResponse(
                patch.getId(),
                patch.getTitle(),
                patch.getVersion(),
                patch.getNotes(),
                patch.getCreatedAt()
        );
    }
}

