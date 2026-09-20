package com.personalassistant.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class UpdateScheduleEntryRequest {

    @NotNull
    private UUID taskId;

    @NotNull
    private Instant startAt;

    @NotNull
    private Instant endAt;
}