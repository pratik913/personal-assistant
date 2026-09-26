import {
  ChangeDetectorRef,
  Component,
  OnInit
} from '@angular/core';

import {
  DatePipe,
  DecimalPipe
} from '@angular/common';

import {
  FormsModule
} from '@angular/forms';

import {
  ActivatedRoute,
  Router
} from '@angular/router';

import {
  Goal,
  GoalProgress,
  GoalService,
  UpdateGoalRequest
} from '../../services/goal.service';

import {
  TaskResponse,
  TaskService,
  TaskStatus
} from '../../services/task.service';

@Component({
  selector: 'app-goal-details',
  imports: [
    DatePipe,
    DecimalPipe,
    FormsModule
  ],
  templateUrl: './goal-details.html',
  styleUrl: './goal-details.scss'
})
export class GoalDetails implements OnInit {

  goal: Goal | null = null;

  tasks: TaskResponse[] = [];

  progress: GoalProgress = {
    totalTasks: 0,
    completedTasks: 0,
    remainingTasks: 0,
    progressPercentage: 0
  };

  loading = true;

  errorMessage = '';

  updatingTaskId: string | null = null;

  unassigningTaskId: string | null = null;

  // =========================================================
  // Edit Goal
  // =========================================================

  showEditGoalModal = false;

  savingGoal = false;

  goalErrorMessage = '';

  editGoal = {
    title: '',
    description: '',
    targetDate: ''
  };

  // =========================================================
  // Delete Goal
  // =========================================================

  showDeleteGoalModal = false;

  deletingGoal = false;

  deleteGoalErrorMessage = '';

  // =========================================================
  // Add New Task
  // =========================================================

  showAddTaskModal = false;

  savingTask = false;

  taskErrorMessage = '';

  newTask = {
    title: '',
    description: '',
    priority: 'MEDIUM' as 'LOW' | 'MEDIUM' | 'HIGH',
    estimatedMinutes: null as number | null
  };

  // =========================================================
  // Add Existing Task
  // =========================================================

  showExistingTasksModal = false;

  unassignedTasks: TaskResponse[] = [];

  loadingExistingTasks = false;

  addingExistingTaskId: string | null = null;

  existingTaskErrorMessage = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly goalService: GoalService,
    private readonly taskService: TaskService,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const goalId = this.route.snapshot.paramMap.get('id');

    if (!goalId) {
      this.errorMessage = 'Goal ID is missing.';
      this.loading = false;
      return;
    }

    this.loadGoal(goalId);
    this.loadTasks(goalId);
    this.loadProgress(goalId);
  }

  private loadGoal(goalId: string): void {
    this.goalService.getGoal(goalId).subscribe({
      next: goal => {
        this.goal = goal;
        this.loading = false;

        this.changeDetectorRef.detectChanges();
      },

      error: error => {
        console.error('Failed to load goal:', error);

        this.errorMessage =
          error?.error?.message ||
          'Failed to load goal.';

        this.loading = false;

        this.changeDetectorRef.detectChanges();
      }
    });
  }

  private loadTasks(goalId: string): void {
    this.goalService.getGoalTasks(goalId).subscribe({
      next: tasks => {
        this.tasks = tasks;

        this.changeDetectorRef.detectChanges();
      },

      error: error => {
        console.error('Failed to load goal tasks:', error);

        this.errorMessage =
          error?.error?.message ||
          'Failed to load goal tasks.';

        this.changeDetectorRef.detectChanges();
      }
    });
  }

  private loadProgress(goalId: string): void {
    this.goalService.getGoalProgress(goalId).subscribe({
      next: progress => {
        this.progress = progress;

        this.changeDetectorRef.detectChanges();
      },

      error: error => {
        console.error('Failed to load goal progress:', error);

        this.changeDetectorRef.detectChanges();
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/goals']);
  }

  openTask(taskId: string): void {
    this.router.navigate(['/tasks', taskId]);
  }

  // =========================================================
  // Edit Goal
  // =========================================================

  openEditGoalModal(): void {
    if (!this.goal) {
      return;
    }

    this.editGoal = {
      title: this.goal.title,
      description: this.goal.description ?? '',
      targetDate: this.goal.targetDate ?? ''
    };

    this.goalErrorMessage = '';

    this.showEditGoalModal = true;
  }

  closeEditGoalModal(): void {
    if (this.savingGoal) {
      return;
    }

    this.showEditGoalModal = false;
    this.goalErrorMessage = '';
  }

  saveGoalChanges(): void {
    if (!this.goal) {
      return;
    }

    const title = this.editGoal.title.trim();
    const description = this.editGoal.description.trim();

    if (!title) {
      this.goalErrorMessage = 'Goal title is required.';
      return;
    }

    if (title.length > 200) {
      this.goalErrorMessage =
        'Goal title must not exceed 200 characters.';
      return;
    }

    if (description.length > 5000) {
      this.goalErrorMessage =
        'Goal description must not exceed 5000 characters.';
      return;
    }

    if (this.editGoal.targetDate) {
      const selectedDate = new Date(
        `${this.editGoal.targetDate}T00:00:00`
      );

      const today = new Date();

      today.setHours(0, 0, 0, 0);

      if (selectedDate < today) {
        this.goalErrorMessage =
          'Target date cannot be in the past.';
        return;
      }
    }

    const request: UpdateGoalRequest = {
      title,
      description
    };

    if (this.editGoal.targetDate) {
      request.targetDate = this.editGoal.targetDate;
    }

    this.savingGoal = true;
    this.goalErrorMessage = '';

    this.goalService
      .updateGoal(this.goal.id, request)
      .subscribe({
        next: updatedGoal => {
          this.goal = updatedGoal;

          this.savingGoal = false;
          this.showEditGoalModal = false;

          this.changeDetectorRef.detectChanges();
        },

        error: error => {
          console.error(
            'Failed to update goal:',
            error
          );

          this.savingGoal = false;

          this.goalErrorMessage =
            error?.error?.message ||
            'Failed to update goal.';

          this.changeDetectorRef.detectChanges();
        }
      });
  }

  // =========================================================
  // Delete Goal
  // =========================================================

  openDeleteGoalModal(): void {
    if (!this.goal) {
      return;
    }

    this.deleteGoalErrorMessage = '';
    this.showDeleteGoalModal = true;
  }

  closeDeleteGoalModal(): void {
    if (this.deletingGoal) {
      return;
    }

    this.showDeleteGoalModal = false;
    this.deleteGoalErrorMessage = '';
  }

  confirmDeleteGoal(): void {
    if (!this.goal || this.deletingGoal) {
      return;
    }

    this.deletingGoal = true;
    this.deleteGoalErrorMessage = '';

    this.goalService
      .deleteGoal(this.goal.id)
      .subscribe({
        next: () => {
          this.deletingGoal = false;
          this.showDeleteGoalModal = false;

          this.router.navigate(['/goals']);
        },

        error: error => {
          console.error(
            'Failed to delete goal:',
            error
          );

          this.deletingGoal = false;

          this.deleteGoalErrorMessage =
            error?.error?.message ||
            'Failed to delete goal.';

          this.changeDetectorRef.detectChanges();
        }
      });
  }

  // =========================================================
  // Add New Task
  // =========================================================

  openAddTaskModal(): void {
    this.newTask = {
      title: '',
      description: '',
      priority: 'MEDIUM',
      estimatedMinutes: null
    };

    this.taskErrorMessage = '';

    this.showAddTaskModal = true;
  }

  closeAddTaskModal(): void {
    if (this.savingTask) {
      return;
    }

    this.showAddTaskModal = false;
    this.taskErrorMessage = '';
  }

  saveNewTask(): void {
    if (!this.goal) {
      return;
    }

    const title = this.newTask.title.trim();

    if (!title) {
      this.taskErrorMessage = 'Task title is required.';
      return;
    }

    this.savingTask = true;
    this.taskErrorMessage = '';

    this.taskService.createTask({
      title,
      description: this.newTask.description.trim(),
      priority: this.newTask.priority,
      estimatedMinutes:
        this.newTask.estimatedMinutes || undefined,
      goalId: this.goal.id
    }).subscribe({
      next: () => {
        this.savingTask = false;
        this.showAddTaskModal = false;

        this.loadTasks(this.goal!.id);
        this.loadProgress(this.goal!.id);

        this.changeDetectorRef.detectChanges();
      },

      error: error => {
        console.error(
          'Failed to create task:',
          error
        );

        this.savingTask = false;

        this.taskErrorMessage =
          error?.error?.message ||
          'Failed to create task.';

        this.changeDetectorRef.detectChanges();
      }
    });
  }

  // =========================================================
  // Add Existing Task
  // =========================================================

  openExistingTasksModal(): void {
    this.showExistingTasksModal = true;

    this.loadingExistingTasks = true;

    this.existingTaskErrorMessage = '';

    this.unassignedTasks = [];

    this.taskService.getTasks().subscribe({
      next: tasks => {
        this.unassignedTasks =
          tasks.filter(task => !task.goalId);

        this.loadingExistingTasks = false;

        this.changeDetectorRef.detectChanges();
      },

      error: error => {
        console.error(
          'Failed to load unassigned tasks:',
          error
        );

        this.loadingExistingTasks = false;

        this.existingTaskErrorMessage =
          error?.error?.message ||
          'Failed to load tasks.';

        this.changeDetectorRef.detectChanges();
      }
    });
  }

  closeExistingTasksModal(): void {
    if (this.addingExistingTaskId) {
      return;
    }

    this.showExistingTasksModal = false;

    this.existingTaskErrorMessage = '';
  }

  addExistingTaskToGoal(task: TaskResponse): void {
    if (!this.goal || this.addingExistingTaskId) {
      return;
    }

    this.addingExistingTaskId = task.id;

    this.existingTaskErrorMessage = '';

    this.taskService.updateTask(
      task.id,
      {
        goalId: this.goal.id
      }
    ).subscribe({
      next: updatedTask => {

        this.tasks = [
          ...this.tasks,
          updatedTask
        ];

        this.unassignedTasks =
          this.unassignedTasks.filter(
            item => item.id !== task.id
          );

        this.addingExistingTaskId = null;

        this.loadProgress(this.goal!.id);

        this.changeDetectorRef.detectChanges();
      },

      error: error => {
        console.error(
          'Failed to add existing task:',
          error
        );

        this.addingExistingTaskId = null;

        this.existingTaskErrorMessage =
          error?.error?.message ||
          'Failed to add task to goal.';

        this.changeDetectorRef.detectChanges();
      }
    });
  }

  // =========================================================
  // Unassign Task
  // =========================================================

  unassignTaskFromGoal(task: TaskResponse): void {
    if (
      !this.goal ||
      this.unassigningTaskId
    ) {
      return;
    }

    const confirmed = window.confirm(
      `Remove "${task.title}" from this goal?`
    );

    if (!confirmed) {
      return;
    }

    this.unassigningTaskId = task.id;

    this.taskService
      .removeTaskFromGoal(task.id)
      .subscribe({
        next: () => {

          this.tasks = this.tasks.filter(
            item => item.id !== task.id
          );

          this.unassignedTasks = [
            ...this.unassignedTasks,
            {
              ...task,
              goalId: null
            }
          ];

          this.unassigningTaskId = null;

          this.loadProgress(this.goal!.id);

          this.changeDetectorRef.detectChanges();
        },

        error: error => {
          console.error(
            'Failed to remove task from goal:',
            error
          );

          this.unassigningTaskId = null;

          this.errorMessage =
            error?.error?.message ||
            'Failed to remove task from goal.';

          this.changeDetectorRef.detectChanges();
        }
      });
  }

  // =========================================================
  // Task Status
  // =========================================================

  toggleTaskStatus(task: TaskResponse): void {
    if (this.updatingTaskId) {
      return;
    }

    const newStatus: TaskStatus =
      task.status === 'COMPLETED'
        ? 'TODO'
        : 'COMPLETED';

    this.updatingTaskId = task.id;

    this.taskService
      .updateTask(task.id, {
        status: newStatus
      })
      .subscribe({
        next: updatedTask => {

          this.tasks = this.tasks.map(item =>
            item.id === updatedTask.id
              ? updatedTask
              : item
          );

          this.updatingTaskId = null;

          if (this.goal) {
            this.loadProgress(this.goal.id);
          }

          this.changeDetectorRef.detectChanges();
        },

        error: error => {
          console.error(
            'Failed to update task status:',
            error
          );

          this.updatingTaskId = null;

          this.errorMessage =
            error?.error?.message ||
            'Failed to update task status.';

          this.changeDetectorRef.detectChanges();
        }
      });
  }

  // =========================================================
  // UI Helpers
  // =========================================================

  getPriorityClass(
    priority: TaskResponse['priority']
  ): string {
    return priority.toLowerCase();
  }

  getStatusLabel(
    status: TaskStatus
  ): string {

    switch (status) {
      case 'COMPLETED':
        return 'Completed';

      case 'IN_PROGRESS':
        return 'In Progress';

      default:
        return 'To Do';
    }
  }
}