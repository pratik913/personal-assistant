import {
  ChangeDetectorRef,
  Component,
  Input,
  OnChanges,
  SimpleChanges,
  inject
} from '@angular/core';

import { DatePipe } from '@angular/common';

import {
  FormsModule
} from '@angular/forms';

import {
  TaskExecutionResponse,
  TaskService
} from '../../services/task.service';

@Component({
  selector: 'app-task-execution',
  imports: [
    DatePipe,
    FormsModule
  ],
  templateUrl: './task-execution.html',
  styleUrl: './task-execution.scss'
})
export class TaskExecution implements OnChanges {

  @Input({ required: true })
  taskId!: string;

  private readonly taskService =
    inject(TaskService);

  private readonly changeDetectorRef =
    inject(ChangeDetectorRef);

  executions: TaskExecutionResponse[] = [];

  activeExecution:
    TaskExecutionResponse | null = null;

  feedback = '';

  isLoading = false;
  isStarting = false;
  isFinishing = false;

  errorMessage = '';
  successMessage = '';

  ngOnChanges(changes: SimpleChanges): void {

    if (
      changes['taskId'] &&
      this.taskId
    ) {
      this.loadExecutions();
    }
  }

  private loadExecutions(): void {

    this.isLoading = true;
    this.errorMessage = '';

    this.taskService
      .getTaskExecutions(this.taskId)
      .subscribe({

        next: (executions) => {

          this.executions = executions;

          this.activeExecution =
            executions.find(
              execution =>
                execution.status === 'STARTED'
            ) ?? null;

          this.isLoading = false;

          this.changeDetectorRef.detectChanges();
        },

        error: (error) => {

          console.error(
            'Failed to load task executions:',
            error
          );

          this.isLoading = false;

          if (error.status === 401) {

            this.errorMessage =
              'Your session has expired. Please log in again.';

          } else if (error.status === 404) {

            this.errorMessage =
              'Task not found or you do not have access to it.';

          } else {

            this.errorMessage =
              'Unable to load execution history.';
          }

          this.changeDetectorRef.detectChanges();
        }
      });
  }

  startExecution(): void {

    if (
      !this.taskId ||
      this.activeExecution ||
      this.isStarting
    ) {
      return;
    }

    this.isStarting = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.taskService
      .startTaskExecution(this.taskId)
      .subscribe({

        next: (execution) => {

          this.activeExecution = execution;

          this.executions = [
            execution,
            ...this.executions
          ];

          this.isStarting = false;

          this.successMessage =
            'Task execution started.';

          this.changeDetectorRef.detectChanges();
        },

        error: (error) => {

          console.error(
            'Failed to start task execution:',
            error
          );

          this.isStarting = false;

          if (error.status === 409) {

            this.errorMessage =
              error.error?.message ??
              'This task already has an active execution.';

            this.loadExecutions();

          } else if (error.status === 401) {

            this.errorMessage =
              'Your session has expired. Please log in again.';

          } else if (error.status === 404) {

            this.errorMessage =
              'Task not found or you do not have access to it.';

          } else {

            this.errorMessage =
              'Unable to start task execution.';
          }

          this.changeDetectorRef.detectChanges();
        }
      });
  }

  finishExecution(): void {

    if (
      !this.taskId ||
      !this.activeExecution ||
      this.isFinishing
    ) {
      return;
    }

    this.isFinishing = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.taskService
      .updateTaskExecution(
        this.taskId,
        this.activeExecution.id,
        {
          status: 'COMPLETED',
          feedback:
            this.feedback.trim() || undefined
        }
      )
      .subscribe({

        next: (updatedExecution) => {

          this.executions =
            this.executions.map(
              execution =>
                execution.id === updatedExecution.id
                  ? updatedExecution
                  : execution
            );

          this.activeExecution = null;

          this.feedback = '';

          this.isFinishing = false;

          this.successMessage =
            'Task execution completed successfully.';

          this.changeDetectorRef.detectChanges();
        },

        error: (error) => {

          console.error(
            'Failed to finish task execution:',
            error
          );

          this.isFinishing = false;

          if (error.status === 401) {

            this.errorMessage =
              'Your session has expired. Please log in again.';

          } else if (error.status === 404) {

            this.errorMessage =
              'Execution not found. Please refresh the page.';

          } else {

            this.errorMessage =
              'Unable to complete task execution.';
          }

          this.changeDetectorRef.detectChanges();
        }
      });
  }
}