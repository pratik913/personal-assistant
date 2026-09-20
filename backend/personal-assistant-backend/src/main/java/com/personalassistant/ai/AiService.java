package com.personalassistant.ai;

import com.personalassistant.dto.AiCaptureAnalysis;
import com.personalassistant.dto.AiPlanData;

public interface AiService {

    AiCaptureAnalysis analyzeCapture(
            String content
    );

    AiPlanData generatePlan(
            String planningContext
    );
}