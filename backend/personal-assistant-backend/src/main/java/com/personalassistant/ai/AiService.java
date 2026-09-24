package com.personalassistant.ai;

import com.personalassistant.dto.AiCaptureAnalysis;
import com.personalassistant.dto.AiPlanData;
import com.personalassistant.dto.ExecutionAnalysis;

public interface AiService {

    AiCaptureAnalysis analyzeCapture(
            String content
    );

    AiPlanData generatePlan(
            String planningContext
    );

    ExecutionAnalysis analyzeExecution(
            String taskTitle,
            String taskDescription,
            Integer estimatedMinutes,
            long actualMinutes,
            String feedback
    );
}