import {
  ChangeDetectorRef,
  Component,
  OnInit
} from '@angular/core';

import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import {
  CreateGoalRequest,
  Goal,
  GoalProgress,
  GoalService
} from '../../services/goal.service';

import {
  CreateTaskRequest,
  TaskPriority,
  TaskService
} from '../../services/task.service';

interface NewGoalTask {
  title: string;
  description: string;
  priority: TaskPriority;
  estimatedMinutes: number | null;
}

@Component({
  selector: 'app-goals',
  imports: [
    DatePipe,
    FormsModule
  ],
  templateUrl: './goals.html',
  styleUrl: './goals.scss',
})
export class Goals implements OnInit {

  goals: Goal[] = [];

  progress: Record<string, GoalProgress> = {};

  loading = false;

  errorMessage = '';

  showCreateForm = false;

  creatingGoal = false;

  createError = '';

  minTargetDate = '';

  showTaskStep = false;

  createdGoal: Goal | null = null;

  savingTasks = false;

  taskError = '';

  newGoal: CreateGoalRequest = {
    title: '',
    description: '',
    targetDate: ''
  };

  newTasks: NewGoalTask[] = [];

  constructor(
    private goalService: GoalService,
    private taskService: TaskService,
    private changeDetectorRef: ChangeDetectorRef,
    private router: Router
  ) {}

  ngOnInit(): void {

    this.minTargetDate =
      this.getTodayDate();

    this.loadGoals();
  }

  private getTodayDate(): string {

    const today = new Date();

    const year =
      today.getFullYear();

    const month =
      String(today.getMonth() + 1)
        .padStart(2, '0');

    const day =
      String(today.getDate())
        .padStart(2, '0');

    return `${year}-${month}-${day}`;
  }

  loadGoals(): void {

    console.log('Loading goals...');

    this.loading = true;

    this.errorMessage = '';

    this.changeDetectorRef
      .detectChanges();

    this.goalService
      .getGoals()
      .subscribe({

        next: (goals) => {

          console.log(
            'Goals received:',
            goals
          );

          this.goals = goals;

          this.loading = false;

          this.changeDetectorRef
            .detectChanges();

          this.loadProgressForGoals();
        },

        error: (error) => {

          console.error(
            'Failed to load goals:',
            error
          );

          this.loading = false;

          this.errorMessage =
            'Unable to load your goals. Please try again.';

          this.changeDetectorRef
            .detectChanges();
        }

      });
  }

  private loadProgressForGoals(): void {

    this.goals.forEach(goal => {

      this.goalService
        .getGoalProgress(goal.id)
        .subscribe({

          next: (progress) => {

            this.progress[goal.id] =
              progress;

            this.changeDetectorRef
              .detectChanges();
          },

          error: (error) => {

            console.error(
              `Failed to load progress for goal ${goal.id}:`,
              error
            );

            this.progress[goal.id] = {
              totalTasks: 0,
              completedTasks: 0,
              remainingTasks: 0,
              progressPercentage: 0
            };

            this.changeDetectorRef
              .detectChanges();
          }

        });

    });
  }

  getProgress(
    goalId: string
  ): GoalProgress {

    return this.progress[goalId] ?? {
      totalTasks: 0,
      completedTasks: 0,
      remainingTasks: 0,
      progressPercentage: 0
    };
  }

  openGoal(goalId: string): void {

    this.router.navigate([
      '/goals',
      goalId
    ]);
  }

  openCreateForm(): void {

    this.showCreateForm = true;

    this.showTaskStep = false;

    this.createdGoal = null;

    this.createError = '';

    this.taskError = '';

    this.newTasks = [];

    this.newGoal = {
      title: '',
      description: '',
      targetDate: ''
    };

    this.changeDetectorRef
      .detectChanges();
  }

  closeCreateForm(): void {

    if (
      this.creatingGoal ||
      this.savingTasks
    ) {
      return;
    }

    this.showCreateForm = false;

    this.showTaskStep = false;

    this.createdGoal = null;

    this.createError = '';

    this.taskError = '';

    this.newTasks = [];

    this.changeDetectorRef
      .detectChanges();
  }

  createGoal(): void {

    this.createError = '';

    const title =
      this.newGoal.title?.trim();

    if (!title) {

      this.createError =
        'Goal title is required.';

      this.changeDetectorRef
        .detectChanges();

      return;
    }

    if (
      this.newGoal.targetDate &&
      this.newGoal.targetDate <
      this.minTargetDate
    ) {

      this.createError =
        'Target date cannot be in the past.';

      this.changeDetectorRef
        .detectChanges();

      return;
    }

    this.creatingGoal = true;

    const request: CreateGoalRequest = {
      title,
      description:
        this.newGoal.description?.trim() || ''
    };

    if (this.newGoal.targetDate) {

      request.targetDate =
        this.newGoal.targetDate;
    }

    console.log(
      'Creating goal:',
      request
    );

    this.goalService
      .createGoal(request)
      .subscribe({

        next: (createdGoal) => {

          console.log(
            'Goal created:',
            createdGoal
          );

          this.createdGoal =
            createdGoal;

          this.goals = [
            createdGoal,
            ...this.goals
          ];

          this.progress[createdGoal.id] = {
            totalTasks: 0,
            completedTasks: 0,
            remainingTasks: 0,
            progressPercentage: 0
          };

          this.creatingGoal = false;

          this.showTaskStep = true;

          this.newTasks = [
            this.createEmptyTask()
          ];

          this.changeDetectorRef
            .detectChanges();
        },

        error: (error) => {

          console.error(
            'Failed to create goal:',
            error
          );

          this.creatingGoal = false;

          this.createError =
            'Unable to create goal. Please try again.';

          this.changeDetectorRef
            .detectChanges();
        }

      });
  }

  private createEmptyTask(): NewGoalTask {

    return {
      title: '',
      description: '',
      priority: 'MEDIUM',
      estimatedMinutes: null
    };
  }

  addTaskRow(): void {

    this.newTasks.push(
      this.createEmptyTask()
    );

    this.changeDetectorRef
      .detectChanges();
  }

  removeTaskRow(index: number): void {

    this.newTasks.splice(index, 1);

    this.changeDetectorRef
      .detectChanges();
  }

  saveTasks(): void {

    if (!this.createdGoal) {
      return;
    }

    this.taskError = '';

    const validTasks =
      this.newTasks.filter(
        task =>
          task.title.trim().length > 0
      );

    if (validTasks.length === 0) {

      this.finishGoalCreation();

      return;
    }

    this.savingTasks = true;

    let completedRequests = 0;

    let hasError = false;

    validTasks.forEach(task => {

      const request: CreateTaskRequest = {

        title:
          task.title.trim(),

        description:
          task.description?.trim() || '',

  

        priority:
          task.priority,

        estimatedMinutes:
          task.estimatedMinutes || undefined,

        goalId:
          this.createdGoal!.id
      };

      this.taskService
        .createTask(request)
        .subscribe({

          next: () => {

            completedRequests++;

            if (
              completedRequests ===
              validTasks.length &&
              !hasError
            ) {

              this.savingTasks = false;

              this.finishGoalCreation();
            }

          },

          error: (error) => {

            console.error(
              'Failed to create goal task:',
              error
            );

            if (!hasError) {

              hasError = true;

              this.savingTasks = false;

              this.taskError =
                'Some tasks could not be created. Please try again.';

              this.changeDetectorRef
                .detectChanges();
            }

          }

        });

    });
  }

  skipTasks(): void {

    this.finishGoalCreation();
  }

  private finishGoalCreation(): void {

    if (!this.createdGoal) {
      return;
    }

    const goalId =
      this.createdGoal.id;

    this.showCreateForm = false;

    this.showTaskStep = false;

    this.newTasks = [];

    this.createdGoal = null;

    this.changeDetectorRef
      .detectChanges();

    this.goalService
      .getGoalProgress(goalId)
      .subscribe({

        next: (progress) => {

          this.progress[goalId] =
            progress;

          this.changeDetectorRef
            .detectChanges();
        }

      });
  }

  retry(): void {

    this.loadGoals();
  }
}