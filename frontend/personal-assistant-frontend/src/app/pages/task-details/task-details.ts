import {
  ChangeDetectorRef,
  Component,
  inject
} from '@angular/core';

import { DatePipe } from '@angular/common';

import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';

import {
  ActivatedRoute,
  Router,
  RouterLink
} from '@angular/router';

import {
  TaskResponse,
  TaskService,
  UpdateTaskRequest
} from '../../services/task.service';

import { TaskExecution }
  from '../../shared/task-execution/task-execution';

@Component({
  selector: 'app-task-details',
  imports: [
    RouterLink,
    DatePipe,
    ReactiveFormsModule,
    TaskExecution
  ],
  templateUrl: './task-details.html',
  styleUrl: './task-details.scss'
})
export class TaskDetails {

  private readonly route =
    inject(ActivatedRoute);

  private readonly router =
    inject(Router);

  private readonly taskService =
    inject(TaskService);

  private readonly changeDetectorRef =
    inject(ChangeDetectorRef);

  task: TaskResponse | null = null;

  isLoading = true;
  isEditing = false;
  isSaving = false;
  isDeleting = false;

  errorMessage = '';

  saveError = '';
  saveSuccess = '';

  deleteError = '';
  showDeleteConfirmation = false;

  taskForm = new FormGroup({

    title: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.maxLength(200)
      ]
    }),

    description: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.maxLength(5000)
      ]
    }),

    priority:
      new FormControl<TaskResponse['priority']>(
        'MEDIUM',
        {
          nonNullable: true,
          validators: [
            Validators.required
          ]
        }
      ),

    dueDate:
      new FormControl<string | null>(null),

    estimatedMinutes:
      new FormControl<number | null>(
        null,
        [
          Validators.min(1)
        ]
      )
  });

  ngOnInit(): void {

    const taskId =
      this.route.snapshot.paramMap.get('id');

    if (!taskId) {

      this.isLoading = false;

      this.errorMessage =
        'Task ID is missing.';

      return;
    }

    this.loadTask(taskId);
  }

  private loadTask(taskId: string): void {

    this.isLoading = true;
    this.errorMessage = '';

    this.taskService
      .getTask(taskId)
      .subscribe({

        next: (task) => {

          this.task = task;
          this.isLoading = false;

          this.populateForm(task);

          this.changeDetectorRef.detectChanges();
        },

        error: (error) => {

          console.error(
            'Failed to load task:',
            error
          );

          this.isLoading = false;

          if (error.status === 404) {

            this.errorMessage =
              'Task not found or you do not have access to it.';

          } else if (error.status === 401) {

            this.errorMessage =
              'Your session has expired. Please log in again.';

          } else {

            this.errorMessage =
              'Unable to load this task.';
          }

          this.changeDetectorRef.detectChanges();
        }
      });
  }

  private populateForm(
    task: TaskResponse
  ): void {

    this.taskForm.patchValue({

      title: task.title,

      description:
        task.description ?? '',

      priority: task.priority,

      dueDate: task.dueDate,

      estimatedMinutes:
        task.estimatedMinutes
    });

    this.taskForm.markAsPristine();
    this.taskForm.markAsUntouched();
  }

  startEditing(): void {

    if (!this.task) {
      return;
    }

    this.populateForm(this.task);

    this.isEditing = true;

    this.saveError = '';
    this.saveSuccess = '';
  }

  cancelEditing(): void {

    if (!this.task) {
      return;
    }

    this.populateForm(this.task);

    this.isEditing = false;
    this.isSaving = false;

    this.saveError = '';
    this.saveSuccess = '';
  }

  saveTask(): void {

    if (!this.task) {
      return;
    }

    if (this.taskForm.invalid) {

      this.taskForm.markAllAsTouched();

      return;
    }

    const request: UpdateTaskRequest = {

      title:
        this.taskForm.controls.title.value.trim(),

      description:
        this.taskForm.controls.description.value.trim(),

      priority:
        this.taskForm.controls.priority.value,

      dueDate:
        this.taskForm.controls.dueDate.value,

      estimatedMinutes:
        this.taskForm.controls
          .estimatedMinutes.value
    };

    this.isSaving = true;

    this.saveError = '';
    this.saveSuccess = '';

    this.taskService
      .updateTask(
        this.task.id,
        request
      )
      .subscribe({

        next: (updatedTask) => {

          this.task = updatedTask;

          this.populateForm(updatedTask);

          this.isEditing = false;
          this.isSaving = false;

          this.saveSuccess =
            'Task updated successfully.';

          this.changeDetectorRef.detectChanges();
        },

        error: (error) => {

          console.error(
            'Failed to update task:',
            error
          );

          this.isSaving = false;

          if (error.status === 400) {

            this.saveError =
              error.error?.message ??
              'Please check the task details.';

          } else if (error.status === 404) {

            this.saveError =
              'Task not found or you do not have access to it.';

          } else if (error.status === 401) {

            this.saveError =
              'Your session has expired. Please log in again.';

          } else {

            this.saveError =
              'Unable to update the task.';
          }

          this.changeDetectorRef.detectChanges();
        }
      });
  }

  openDeleteConfirmation(): void {

    if (!this.task || this.isDeleting) {
      return;
    }

    this.deleteError = '';
    this.showDeleteConfirmation = true;
  }

  cancelDelete(): void {

    if (this.isDeleting) {
      return;
    }

    this.showDeleteConfirmation = false;
    this.deleteError = '';
  }

  confirmDelete(): void {

    if (!this.task || this.isDeleting) {
      return;
    }

    this.isDeleting = true;
    this.deleteError = '';

    this.taskService
      .deleteTask(this.task.id)
      .subscribe({

        next: () => {

          this.isDeleting = false;
          this.showDeleteConfirmation = false;

          this.router.navigate(['/tasks']);
        },

        error: (error) => {

          console.error(
            'Failed to delete task:',
            error
          );

          this.isDeleting = false;

          if (error.status === 404) {

            this.deleteError =
              'Task not found or you do not have access to it.';

          } else if (error.status === 401) {

            this.deleteError =
              'Your session has expired. Please log in again.';

          } else {

            this.deleteError =
              'Unable to delete the task. Please try again.';
          }

          this.changeDetectorRef.detectChanges();
        }
      });
  }

  getPriorityClass(
    priority: string
  ): string {

    return priority.toLowerCase();
  }

  getStatusClass(
    status: string
  ): string {

    return status
      .toLowerCase()
      .replace('_', '-');
  }
} 