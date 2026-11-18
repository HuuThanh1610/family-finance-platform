package com.familyfinance.family.service;

import com.familyfinance.family.entity.FamilyAuditLog;
import com.familyfinance.family.repository.FamilyAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class FamilyAuditService {

    private final FamilyAuditLogRepository repository;

    public FamilyAuditService(FamilyAuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID familyId, UUID actorUserId, String action, String entityType, UUID entityId,
                       Map<String, Object> metadata) {
        FamilyAuditLog log = new FamilyAuditLog();
        log.setFamilyId(familyId);
        log.setActorUserId(actorUserId);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setMetadata(metadata != null ? metadata : Map.of());
        repository.save(log);
    }
}