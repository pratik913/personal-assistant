package com.personalassistant.dto;

import com.personalassistant.entity.CaptureType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCaptureRequest {

    @NotNull
    private CaptureType type;

    @Size(max = 5000)
    private String content;

    @Size(max = 2000)
    private String sourceUrl;
}