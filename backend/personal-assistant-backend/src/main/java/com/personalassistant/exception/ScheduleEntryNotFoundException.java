package com.personalassistant.exception;

public class ScheduleEntryNotFoundException
        extends RuntimeException {

    public ScheduleEntryNotFoundException(String message) {
        super(message);
    }
}