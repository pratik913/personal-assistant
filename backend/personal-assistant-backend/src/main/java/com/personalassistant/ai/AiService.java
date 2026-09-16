package com.personalassistant.ai;

import com.personalassistant.dto.AiCaptureAnalysis;

public interface AiService {

    AiCaptureAnalysis analyzeCapture(String content);

}