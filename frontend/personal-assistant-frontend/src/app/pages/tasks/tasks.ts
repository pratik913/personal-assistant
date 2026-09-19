import {
  ChangeDetectorRef,
  Component,
  inject
} from '@angular/core';
import { RouterLink } from '@angular/router';

import {
  TaskResponse,
  TaskService
} from '../../services/task.service';

@Component({
  selector: 'app-tasks',
  imports: [RouterLink],
  templateUrl: './tasks.html',
  styleUrl: './tasks.scss'
})
export class Tasks {
  private readonly taskService = inject(TaskService);
  private readonly changeDetectorRef =
    inject(ChangeDetectorRef);

  tasks: TaskResponse[] = [];

  isLoading = true;
  errorMessage = '';

  ngOnInit(): void {
    this.loadTasks();
  }

  loadTasks(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.taskService.getTasks().subscribe({
      next: (tasks) => {
        console.log('Tasks loaded:', tasks);

        this.tasks = tasks;
        this.isLoading = false;

        this.changeDetectorRef.detectChanges();
      },

      error: (error) => {
        console.error(
          'Failed to load tasks:',
          error
        );

        this.isLoading = false;

        if (error.status === 401) {
          this.errorMessage =
            'Your session has expired. Please log in again.';
        } else {
          this.errorMessage =
            'Unable to load your tasks. Please try again.';
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
    return status.toLowerCase().replace('_', '-');
  }
}