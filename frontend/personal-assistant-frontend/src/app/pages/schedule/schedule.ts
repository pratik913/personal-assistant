import {
  ChangeDetectorRef,
  Component,
  OnInit
} from '@angular/core';

import {
  FormsModule
} from '@angular/forms';

import {
  TaskResponse,
  TaskService
} from '../../services/task.service';

import {
  ScheduleEntryResponse,
  ScheduleService
} from '../../services/schedule.service';

interface CalendarEvent {
  id: string;
  taskId: string;
  title: string;
  date: string;
  startTime: string;
  endTime: string;
}

@Component({
  selector: 'app-schedule',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './schedule.html',
  styleUrl: './schedule.scss'
})
export class ScheduleComponent implements OnInit {

  selectedDate = this.formatDate(new Date());

  showAddForm = false;

  isLoading = true;

  isSaving = false;

  errorMessage = '';

  tasks: TaskResponse[] = [];

  events: CalendarEvent[] = [];

  newEvent = {
    taskId: '',
    startTime: '09:00',
    endTime: '10:00'
  };

  constructor(
    private readonly scheduleService: ScheduleService,
    private readonly taskService: TaskService,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadTasks();
    this.loadScheduleEntries();
  }

  get selectedDateLabel(): string {

    const date =
      new Date(
        `${this.selectedDate}T00:00:00`
      );

    return date.toLocaleDateString(
      'en-IN',
      {
        weekday: 'long',
        month: 'long',
        day: 'numeric'
      }
    );
  }

  get selectedEvents(): CalendarEvent[] {

    return this.events
      .filter(
        event =>
          event.date ===
          this.selectedDate
      )
      .sort(
        (a, b) =>
          a.startTime.localeCompare(
            b.startTime
          )
      );
  }

  get previousDate(): string {

    return this.changeDate(
      this.selectedDate,
      -1
    );
  }

  get nextDate(): string {

    return this.changeDate(
      this.selectedDate,
      1
    );
  }

  selectDate(
    date: string
  ): void {

    this.selectedDate = date;

    this.changeDetectorRef.detectChanges();
  }

  goPreviousDay(): void {

    this.selectedDate =
      this.previousDate;

    this.changeDetectorRef.detectChanges();
  }

  goNextDay(): void {

    this.selectedDate =
      this.nextDate;

    this.changeDetectorRef.detectChanges();
  }

  goToday(): void {

    this.selectedDate =
      this.formatDate(
        new Date()
      );

    this.changeDetectorRef.detectChanges();
  }

  openAddForm(): void {

    console.log(
      '[Schedule] Add task button clicked'
    );

    console.log(
      '[Schedule] Current tasks:',
      this.tasks
    );

    this.errorMessage = '';

    if (this.tasks.length === 0) {

      this.errorMessage =
        'No active tasks are available to schedule.';

      this.changeDetectorRef.detectChanges();

      return;
    }

    this.newEvent = {
      taskId: this.tasks[0].id,
      startTime: '09:00',
      endTime: '10:00'
    };

    this.showAddForm = true;

    console.log(
      '[Schedule] showAddForm =',
      this.showAddForm
    );

    console.log(
      '[Schedule] selected task =',
      this.newEvent.taskId
    );

    this.changeDetectorRef.detectChanges();
  }

  closeAddForm(): void {

    if (this.isSaving) {
      return;
    }

    this.showAddForm = false;

    this.changeDetectorRef.detectChanges();
  }

  addEvent(): void {

    this.errorMessage = '';

    if (!this.newEvent.taskId) {

      this.errorMessage =
        'Please select a task.';

      this.changeDetectorRef.detectChanges();

      return;
    }

    if (
      this.newEvent.endTime <=
      this.newEvent.startTime
    ) {

      this.errorMessage =
        'End time must be after start time.';

      this.changeDetectorRef.detectChanges();

      return;
    }

    this.isSaving = true;

    const request = {
      taskId: this.newEvent.taskId,
      startAt: this.toInstant(
        this.selectedDate,
        this.newEvent.startTime
      ),
      endAt: this.toInstant(
        this.selectedDate,
        this.newEvent.endTime
      )
    };

    console.log(
      '[Schedule] Creating schedule entry:',
      request
    );

    this.changeDetectorRef.detectChanges();

    this.scheduleService
      .createScheduleEntry(request)
      .subscribe({

        next: response => {

          console.log(
            '[Schedule] Schedule entry created:',
            response
          );

          this.isSaving = false;
          this.showAddForm = false;

          this.changeDetectorRef.detectChanges();

          this.loadScheduleEntries();
        },

        error: error => {

          console.error(
            '[Schedule] Failed to create schedule entry:',
            error
          );

          this.isSaving = false;

          this.errorMessage =
            this.getErrorMessage(
              error,
              'Unable to add this schedule entry.'
            );

          this.changeDetectorRef.detectChanges();
        }
      });
  }

  deleteEvent(
    id: string
  ): void {

    this.errorMessage = '';

    this.scheduleService
      .deleteScheduleEntry(id)
      .subscribe({

        next: () => {

          this.events =
            this.events.filter(
              event =>
                event.id !== id
            );

          this.changeDetectorRef.detectChanges();
        },

        error: error => {

          console.error(
            '[Schedule] Failed to delete schedule entry:',
            error
          );

          this.errorMessage =
            this.getErrorMessage(
              error,
              'Unable to delete this schedule entry.'
            );

          this.changeDetectorRef.detectChanges();
        }
      });
  }

  private loadTasks(): void {

    console.log(
      '[Schedule] Loading tasks...'
    );

    this.taskService
      .getTasks()
      .subscribe({

        next: tasks => {

          console.log(
            '[Schedule] Tasks received:',
            tasks
          );

          this.tasks = tasks.filter(
            task =>
              task.status !== 'COMPLETED' &&
              task.status !== 'CANCELLED'
          );

          console.log(
            '[Schedule] Active tasks:',
            this.tasks
          );

          this.changeDetectorRef.detectChanges();
        },

        error: error => {

          console.error(
            '[Schedule] Failed to load tasks:',
            error
          );

          this.errorMessage =
            this.getErrorMessage(
              error,
              'Unable to load your tasks.'
            );

          this.changeDetectorRef.detectChanges();
        }
      });
  }

  private loadScheduleEntries(): void {

    console.log(
      '[Schedule] Loading schedule entries...'
    );

    this.isLoading = true;

    this.changeDetectorRef.detectChanges();

    this.scheduleService
      .getScheduleEntries()
      .subscribe({

        next: entries => {

          console.log(
            '[Schedule] Schedule entries received:',
            entries
          );

          this.events =
            entries.map(
              entry =>
                this.toCalendarEvent(entry)
            );

          this.isLoading = false;

          console.log(
            '[Schedule] isLoading =',
            this.isLoading
          );

          this.changeDetectorRef.detectChanges();
        },

        error: error => {

          console.error(
            '[Schedule] Failed to load schedule:',
            error
          );

          this.isLoading = false;

          this.errorMessage =
            this.getErrorMessage(
              error,
              'Unable to load your schedule.'
            );

          this.changeDetectorRef.detectChanges();
        }
      });
  }

  private toCalendarEvent(
    entry: ScheduleEntryResponse
  ): CalendarEvent {

    const start =
      new Date(entry.startAt);

    const end =
      new Date(entry.endAt);

    return {
      id: entry.id,
      taskId: entry.taskId,
      title: entry.taskTitle,
      date: this.formatDate(start),
      startTime: this.formatTime(start),
      endTime: this.formatTime(end)
    };
  }

  private toInstant(
    date: string,
    time: string
  ): string {

    const localDate =
      new Date(
        `${date}T${time}:00`
      );

    return localDate.toISOString();
  }

  private formatTime(
    date: Date
  ): string {

    return date.toLocaleTimeString(
      'en-IN',
      {
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
      }
    );
  }

  private formatDate(
    date: Date
  ): string {

    const year =
      date.getFullYear();

    const month =
      String(
        date.getMonth() + 1
      ).padStart(2, '0');

    const day =
      String(
        date.getDate()
      ).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }

  private changeDate(
    current: string,
    amount: number
  ): string {

    const date =
      new Date(
        `${current}T00:00:00`
      );

    date.setDate(
      date.getDate() + amount
    );

    return this.formatDate(date);
  }

  private getErrorMessage(
    error: any,
    fallback: string
  ): string {

    if (
      error?.error?.message
    ) {

      return error.error.message;
    }

    if (
      typeof error?.error === 'string' &&
      error.error.trim()
    ) {

      return error.error;
    }

    if (error?.status === 400) {

      return 'Please check the schedule details.';
    }

    if (error?.status === 401) {

      return 'Your session has expired. Please sign in again.';
    }

    if (error?.status === 0) {

      return 'Unable to connect to MindMate backend.';
    }

    return fallback;
  }
}