package com.personalassistant.service;

import com.personalassistant.dto.CreateScheduleEntryRequest;
import com.personalassistant.dto.ScheduleEntryResponse;
import com.personalassistant.dto.UpdateScheduleEntryRequest;
import com.personalassistant.entity.ScheduleEntry;
import com.personalassistant.entity.Task;
import com.personalassistant.entity.User;
import com.personalassistant.exception.InvalidScheduleTimeException;
import com.personalassistant.exception.ScheduleEntryNotFoundException;
import com.personalassistant.mapper.ScheduleEntryMapper;
import com.personalassistant.repository.ScheduleEntryRepository;
import com.personalassistant.repository.TaskRepository;
import com.personalassistant.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ScheduleEntryService {

    private final ScheduleEntryRepository scheduleEntryRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ScheduleEntryMapper scheduleEntryMapper;

    public ScheduleEntryService(
            ScheduleEntryRepository scheduleEntryRepository,
            TaskRepository taskRepository,
            UserRepository userRepository,
            ScheduleEntryMapper scheduleEntryMapper
    ) {
        this.scheduleEntryRepository = scheduleEntryRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.scheduleEntryMapper = scheduleEntryMapper;
    }

    public ScheduleEntryResponse createScheduleEntry(
            UUID userId,
            CreateScheduleEntryRequest request
    ) {
        validateTimeRange(
                request.getStartAt(),
                request.getEndAt()
        );

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found."
                        )
                );

        Task task = taskRepository
                .findByIdAndUserId(
                        request.getTaskId(),
                        userId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Task not found or you do not have access to it."
                        )
                );

        ScheduleEntry scheduleEntry =
                new ScheduleEntry();

        scheduleEntry.setUser(user);
        scheduleEntry.setTask(task);
        scheduleEntry.setStartAt(request.getStartAt());
        scheduleEntry.setEndAt(request.getEndAt());

        ScheduleEntry savedScheduleEntry =
                scheduleEntryRepository.save(scheduleEntry);

        return scheduleEntryMapper.toResponse(
                savedScheduleEntry
        );
    }

    public List<ScheduleEntryResponse> getScheduleEntries(
            UUID userId
    ) {
        return scheduleEntryRepository
                .findByUserId(userId)
                .stream()
                .map(scheduleEntryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScheduleEntryResponse getScheduleEntry(
            UUID scheduleEntryId,
            UUID userId
    ) {
        ScheduleEntry scheduleEntry =
                scheduleEntryRepository
                        .findByIdAndUserId(
                                scheduleEntryId,
                                userId
                        )
                        .orElseThrow(() ->
                                new ScheduleEntryNotFoundException(
                                        "Schedule entry not found or you do not have access to it."
                                )
                        );

        return scheduleEntryMapper.toResponse(
                scheduleEntry
        );
    }

    public ScheduleEntryResponse updateScheduleEntry(
            UUID scheduleEntryId,
            UUID userId,
            UpdateScheduleEntryRequest request
    ) {
        validateTimeRange(
                request.getStartAt(),
                request.getEndAt()
        );

        ScheduleEntry scheduleEntry =
                scheduleEntryRepository
                        .findByIdAndUserId(
                                scheduleEntryId,
                                userId
                        )
                        .orElseThrow(() ->
                                new ScheduleEntryNotFoundException(
                                        "Schedule entry not found or you do not have access to it."
                                )
                        );

        Task task = taskRepository
                .findByIdAndUserId(
                        request.getTaskId(),
                        userId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Task not found or you do not have access to it."
                        )
                );

        scheduleEntry.setTask(task);
        scheduleEntry.setStartAt(request.getStartAt());
        scheduleEntry.setEndAt(request.getEndAt());

        ScheduleEntry updatedScheduleEntry =
                scheduleEntryRepository.saveAndFlush(scheduleEntry);

        return scheduleEntryMapper.toResponse(
                updatedScheduleEntry
        );
    }

    public void deleteScheduleEntry(
            UUID scheduleEntryId,
            UUID userId
    ) {
        ScheduleEntry scheduleEntry =
                scheduleEntryRepository
                        .findByIdAndUserId(
                                scheduleEntryId,
                                userId
                        )
                        .orElseThrow(() ->
                                new ScheduleEntryNotFoundException(
                                        "Schedule entry not found or you do not have access to it."
                                )
                        );

        scheduleEntryRepository.delete(scheduleEntry);
    }

    private void validateTimeRange(
            Instant startAt,
            Instant endAt
    ) {
        if (!endAt.isAfter(startAt)) {
            throw new InvalidScheduleTimeException(
                    "End time must be after start time."
            );
        }
    }
}