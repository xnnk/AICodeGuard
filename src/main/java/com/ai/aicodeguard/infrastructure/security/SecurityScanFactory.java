package com.ai.aicodeguard.infrastructure.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: SecurityScanFactory
 * @Description: 安全扫描工厂
 * @Author: LZX
 * @Date: 2025/4/27 17:48
 */
@Component
public class SecurityScanFactory {

    private final Map<SecurityScanType, SecurityScanService> serviceMap = new HashMap<>();

    @Autowired
    public SecurityScanFactory(List<SecurityScanService> services) {
        for (SecurityScanService service : services) {
            serviceMap.put(service.getType(), service);
        }
    }

    public SecurityScanService getService(SecurityScanType type) {
        SecurityScanService service = serviceMap.get(type);
        if (service == null) {
            throw new IllegalArgumentException("不支持的安全扫描类型: " + type);
        }
        return service;
    }

    public SecurityScanService getDefaultService() {
        return serviceMap.get(SecurityScanType.AI_MODEL);
    }
}
