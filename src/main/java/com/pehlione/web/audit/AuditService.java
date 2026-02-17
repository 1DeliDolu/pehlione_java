package com.pehlione.web.audit;

import org.springframework.stereotype.Service;

import com.pehlione.web.auth.AuthSecurityEventService;
import com.pehlione.web.auth.ClientInfo;
import com.pehlione.web.user.User;

@Service
public class AuditService {

	private final AuthSecurityEventService securityEventService;

	public AuditService(AuthSecurityEventService securityEventService) {
		this.securityEventService = securityEventService;
	}

	public void record(User user, String type, String entityType, String entityId, ClientInfo client, String details) {
		String payload = "entityType=" + entityType + ", entityId=" + entityId;
		if (details != null && !details.isBlank()) {
			payload = payload + ", " + details;
		}
		securityEventService.record(user, type, client, payload);
	}
}
