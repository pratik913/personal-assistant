package com.personalassistant.controller;

import com.personalassistant.ai.AiService;
import com.personalassistant.dto.AiCaptureAnalysis;
import com.personalassistant.dto.AiTestRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiTestController {

    private final AiService aiService;

    public AiTestController(AiService aiService) {
        this.aiService = aiService;
    }

}