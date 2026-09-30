package ams.asmtravel.api.services.audit.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.audit.dto.request.AuditSearchRequest;
import ams.asmtravel.api.services.audit.dto.response.AuditLogResponse;
import ams.asmtravel.api.services.audit.service.AuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Entries cannot be edited or removed one by one; full control can clear all, which leaves a PURGE entry. */
@Tag(name = "System · Audit log", description = "Who changed what, from where, and what it changed from.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/audit")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public List<AuditLogResponse> search(@ParameterObject @Valid @ModelAttribute AuditSearchRequest request) {
        return auditService.search(request);
    }

    /** All or nothing; returns the PURGE entry, whose {@code newData.removed} says how many went. */
    @DeleteMapping
    public AuditLogResponse clear() {
        return auditService.clearAll();
    }
}
