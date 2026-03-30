package com.vyms.service;

import com.vyms.entity.SystemLog;
import com.vyms.repository.SystemLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SystemLogService {

    private final SystemLogRepository systemLogRepository;

    @Autowired
    public SystemLogService(SystemLogRepository systemLogRepository) {
        this.systemLogRepository = systemLogRepository;
    }

    public List<SystemLog> findAllLogs() {
        return systemLogRepository.findAll();
    }

    public void createLog(String type, String description, String username, String status) {
        SystemLog log = new SystemLog();
        log.setType(type);
        log.setDescription(description);
        log.setUser(username);
        log.setTimestamp(LocalDateTime.now());
        log.setStatus(status);
        systemLogRepository.save(log);
    }
}
