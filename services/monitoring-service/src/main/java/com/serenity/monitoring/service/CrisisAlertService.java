package com.serenity.monitoring.service;

import com.serenity.monitoring.dto.CrisisAlertPayload;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface CrisisAlertService {

	SseEmitter subscribe(Long doctorId);

	void sendCrisisAlert(CrisisAlertPayload payload);
}

