package com.personalassistant.mapper;

import com.personalassistant.dto.ScheduleEntryResponse;
import com.personalassistant.entity.ScheduleEntry;
import org.springframework.stereotype.Component;

@Component
public class ScheduleEntryMapper {

    public ScheduleEntryResponse toResponse(
            ScheduleEntry scheduleEntry
    ) {
        return new ScheduleEntryResponse(
                scheduleEntry.getId(),
                scheduleEntry.getTask().getId(),
                scheduleEntry.getTask().getTitle(),
                scheduleEntry.getStartAt(),
                scheduleEntry.getEndAt(),
                scheduleEntry.getCreatedAt(),
                scheduleEntry.getUpdatedAt()
        );
    }
}