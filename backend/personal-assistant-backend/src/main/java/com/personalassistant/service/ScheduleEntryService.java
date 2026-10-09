package com.personalassistant.service;

import com.personalassistant.dto.CreateScheduleEntryRequest;
import com.personalassistant.dto.ScheduleEntryResponse;
import com.personalassistant.dto.UpdateScheduleEntryRequest;
import com.personalassistant.entity.ScheduleEntry;
import com.personalassistant.entity.Task;
import com.personalassistant.entity.User;
import com.personalassistant.exception.ConflictException;
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
        this.scheduleEntryRepository =
                scheduleEntryRepository;

        this.taskRepository =
                taskRepository;

        this.userRepository =
                userRepository;

        this.scheduleEntryMapper =
                scheduleEntryMapper;
    }

    /**
     * Creates a schedule entry for the authenticated user.
     *
     * Validation performed here:
     *
     * - request must exist
     * - start/end must be valid
     * - user must exist
     * - task must belong to the authenticated user
     * - schedule must not overlap an existing entry
     */
    public ScheduleEntryResponse createScheduleEntry(
            UUID userId,
            CreateScheduleEntryRequest request
    ) {

        validateRequest(request);

        validateTimeRange(
                request.getStartAt(),
                request.getEndAt()
        );

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "User not found."
                                )
                        );

        Task task =
                taskRepository
                        .findByIdAndUserId(
                                request.getTaskId(),
                                userId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Task not found or you do not have access to it."
                                )
                        );

        validateNoOverlap(
                userId,
                request.getStartAt(),
                request.getEndAt()
        );

        ScheduleEntry scheduleEntry =
                new ScheduleEntry();

        scheduleEntry.setUser(user);

        scheduleEntry.setTask(task);

        scheduleEntry.setStartAt(
                request.getStartAt()
        );

        scheduleEntry.setEndAt(
                request.getEndAt()
        );

        ScheduleEntry savedScheduleEntry =
                scheduleEntryRepository.save(
                        scheduleEntry
                );

        return scheduleEntryMapper.toResponse(
                savedScheduleEntry
        );
    }

    /**
     * Returns all schedule entries belonging to the
     * authenticated user.
     */
    @Transactional(readOnly = true)
    public List<ScheduleEntryResponse> getScheduleEntries(
            UUID userId
    ) {

        return scheduleEntryRepository
                .findByUserId(userId)
                .stream()
                .map(scheduleEntryMapper::toResponse)
                .toList();
    }

    /**
     * Returns one schedule entry only when it belongs
     * to the authenticated user.
     */
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

    /**
     * Updates an existing schedule entry.
     *
     * The authenticated user's ownership is checked
     * through findByIdAndUserId().
     */
    public ScheduleEntryResponse updateScheduleEntry(
            UUID scheduleEntryId,
            UUID userId,
            UpdateScheduleEntryRequest request
    ) {

        validateRequest(request);

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

        Task task =
                taskRepository
                        .findByIdAndUserId(
                                request.getTaskId(),
                                userId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Task not found or you do not have access to it."
                                )
                        );

        validateNoOverlapForUpdate(
                userId,
                scheduleEntryId,
                request.getStartAt(),
                request.getEndAt()
        );

        scheduleEntry.setTask(task);

        scheduleEntry.setStartAt(
                request.getStartAt()
        );

        scheduleEntry.setEndAt(
                request.getEndAt()
        );

        ScheduleEntry updatedScheduleEntry =
                scheduleEntryRepository.saveAndFlush(
                        scheduleEntry
                );

        return scheduleEntryMapper.toResponse(
                updatedScheduleEntry
        );
    }

    /**
     * Deletes a schedule entry only when it belongs
     * to the authenticated user.
     */
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

        scheduleEntryRepository.delete(
                scheduleEntry
        );
    }

    /**
     * Validates overlap during creation.
     */
    private void validateNoOverlap(
            UUID userId,
            Instant startAt,
            Instant endAt
    ) {

        boolean hasOverlap =
                !scheduleEntryRepository
                        .findOverlappingEntries(
                                userId,
                                startAt,
                                endAt
                        )
                        .isEmpty();

        if (hasOverlap) {
            throw new ConflictException(
                    "Schedule entry overlaps with an existing schedule."
            );
        }
    }

    /**
     * Validates overlap during update while excluding
     * the schedule entry currently being updated.
     */
    private void validateNoOverlapForUpdate(
            UUID userId,
            UUID scheduleEntryId,
            Instant startAt,
            Instant endAt
    ) {

        boolean hasOverlap =
                !scheduleEntryRepository
                        .findOverlappingEntriesExcludingId(
                                userId,
                                scheduleEntryId,
                                startAt,
                                endAt
                        )
                        .isEmpty();

        if (hasOverlap) {
            throw new ConflictException(
                    "Schedule entry overlaps with an existing schedule."
            );
        }
    }

    /**
     * Validates request object.
     */
    private void validateRequest(
            CreateScheduleEntryRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Schedule request is required."
            );
        }

        if (request.getTaskId() == null) {
            throw new IllegalArgumentException(
                    "Task is required."
            );
        }

        if (request.getStartAt() == null) {
            throw new InvalidScheduleTimeException(
                    "Start time is required."
            );
        }

        if (request.getEndAt() == null) {
            throw new InvalidScheduleTimeException(
                    "End time is required."
            );
        }
    }

    /**
     * Overload for update requests.
     */
    private void validateRequest(
            UpdateScheduleEntryRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Schedule request is required."
            );
        }

        if (request.getTaskId() == null) {
            throw new IllegalArgumentException(
                    "Task is required."
            );
        }

        if (request.getStartAt() == null) {
            throw new InvalidScheduleTimeException(
                    "Start time is required."
            );
        }

        if (request.getEndAt() == null) {
            throw new InvalidScheduleTimeException(
                    "End time is required."
            );
        }
    }

    /**
     * End must always be strictly after start.
     */
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