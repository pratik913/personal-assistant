package com.personalassistant.mapper;

import com.personalassistant.dto.ExecutionAnalysisResponse;
import com.personalassistant.entity.ExecutionAnalysis;
import org.springframework.stereotype.Component;

@Component
public class ExecutionAnalysisMapper {

    public ExecutionAnalysisResponse toResponse(
            ExecutionAnalysis analysis
    ) {
        return new ExecutionAnalysisResponse(
                analysis.getId(),
                analysis.getExecution().getId(),
                analysis.getDifficulty(),
                analysis.getTimeAssessment(),
                analysis.getBlocker(),
                analysis.getInsight(),
                analysis.getSuggestion(),
                analysis.getCreatedAt()
        );
    }
}