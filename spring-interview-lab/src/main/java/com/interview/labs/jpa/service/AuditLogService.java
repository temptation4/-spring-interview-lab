package com.interview.labs.jpa.service;

import com.interview.labs.jpa.entity.AuditLog;
import com.interview.labs.jpa.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // Runs in its own transaction — commits independently even if the
    // caller's transaction later rolls back.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String message) {
        AuditLog auditLog = new AuditLog();
        auditLog.setMessage(message);
        auditLogRepository.save(auditLog);
    }
}
