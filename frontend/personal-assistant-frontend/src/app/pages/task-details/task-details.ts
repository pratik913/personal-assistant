import {
  ChangeDetectorRef,
  Component,
  inject
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import {
  TaskResponse,
  TaskService
} from '../../services/task.service';

import { DatePipe } from '@angular/common';
@Component({
  selector: 'app-task-details',
  imports: [RouterLink, DatePipe],
  templateUrl: './task-details.html',
  styleUrl: './task-details.scss'
})
export class TaskDetails {
  private readonly route = inject(ActivatedRoute);
  private readonly taskService = inject(TaskService);
  private readonly changeDetectorRef =
    inject(ChangeDetectorRef);

  task: TaskResponse | null = null;

  isLoading = true;
  errorMessage = '';

  ngOnInit(): void {
    const taskId = this.route.snapshot.paramMap.get('id');

    if (!taskId) {
      this.isLoading = false;
      this.errorMessage = 'Task ID is missing.';
      return;
    }

    this.loadTask(taskId);
  }

  private loadTask(taskId: string): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.taskService.getTask(taskId).subscribe({
      next: (task) => {
        console.log('Task loaded:', task);

        this.task = task;
        this.isLoading = false;

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