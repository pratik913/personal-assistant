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
  ExecutionAnalysisResponse,
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

  /**
   * Stores AI analysis against execution id.
   *
   * Example:
   * {
   *   "execution-id-1": {
   *      difficulty: "EASY",
   *      ...
   *   }
   * }
   */
  executionAnalyses:
    Record<string, ExecutionAnalysisResponse> = {};

  /**
   * Tracks executions for which analysis
   * does not exist yet.
   */
  executionsWithoutAnalysis =
    new Set<string>();

  /**
   * Tracks executions currently being
   * analyzed by AI.
   */
  analyzingExecutionIds =
    new Set<string>();

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

          /*
           * Whenever executions are loaded,
           * check completed executions for
           * previously generated AI analysis.
           */
          this.loadExistingAnalyses(executions);

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

  /**
   * Loads already persisted AI analyses.
   *
   * Important:
   * We do NOT automatically call the POST analysis
   * endpoint here.
   *
   * This prevents OpenAI from being called every
   * time the user refreshes the page.
   */
  private loadExistingAnalyses(
    executions: TaskExecutionResponse[]
  ): void {

    const completedExecutions =
      executions.filter(
        execution =>
          execution.status === 'COMPLETED'
      );

    completedExecutions.forEach(
      execution => {

        /*
         * Avoid unnecessary API calls when
         * analysis is already loaded.
         */
        if (
          this.executionAnalyses[execution.id]
        ) {
          return;
        }

        this.taskService
          .getExecutionAnalysis(
            this.taskId,
            execution.id
          )
          .subscribe({

            next: (analysis) => {

              this.executionAnalyses[
                execution.id
              ] = analysis;

              this.executionsWithoutAnalysis.delete(
                execution.id
              );

              this.changeDetectorRef.detectChanges();
            },

            error: (error) => {

              /*
               * 404 means:
               * execution exists but AI analysis
               * has not been generated yet.
               *
               * This is NOT treated as a UI error.
               */
              if (error.status === 404) {

                this.executionsWithoutAnalysis.add(
                  execution.id
                );

                this.changeDetectorRef.detectChanges();

                return;
              }

              console.error(
                `Failed to load analysis for execution ${execution.id}:`,
                error
              );
            }
          });
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

          /*
           * A newly completed execution will not
           * have AI analysis yet.
           */
          this.executionsWithoutAnalysis.add(
            updatedExecution.id
          );

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

  /**
   * Generates AI analysis for a completed execution.
   *
   * This method is intentionally triggered
   * by the user through the UI.
   *
   * It does NOT run automatically.
   */
  analyzeExecution(
    execution: TaskExecutionResponse
  ): void {

    if (
      execution.status !== 'COMPLETED' ||
      this.isAnalyzing(execution.id)
    ) {
      return;
    }

    /*
     * If analysis is already available,
     * do not call OpenAI again.
     */
    if (
      this.executionAnalyses[execution.id]
    ) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.analyzingExecutionIds.add(
      execution.id
    );

    this.changeDetectorRef.detectChanges();

    this.taskService
      .analyzeExecution(
        this.taskId,
        execution.id
      )
      .subscribe({

        next: (analysis) => {

          this.executionAnalyses[
            execution.id
          ] = analysis;

          this.executionsWithoutAnalysis.delete(
            execution.id
          );

          this.analyzingExecutionIds.delete(
            execution.id
          );

          this.successMessage =
            'AI execution analysis generated successfully.';

          this.changeDetectorRef.detectChanges();
        },

        error: (error) => {

          console.error(
            'Failed to analyze execution:',
            error
          );

          this.analyzingExecutionIds.delete(
            execution.id
          );

          if (error.status === 401) {

            this.errorMessage =
              'Your session has expired. Please log in again.';

          } else if (error.status === 404) {

            this.errorMessage =
              'Execution not found. Please refresh the page.';

          } else if (error.status === 409) {

            this.errorMessage =
              error.error?.message ??
              'This execution cannot be analyzed yet.';

          } else {

            this.errorMessage =
              'Unable to generate AI analysis. Please try again.';
          }

          this.changeDetectorRef.detectChanges();
        }
      });
  }

  /**
   * Returns whether an execution currently
   * has an AI analysis.
   */
  hasAnalysis(
    executionId: string
  ): boolean {

    return !!this.executionAnalyses[
      executionId
    ];
  }

  /**
   * Returns whether an execution is currently
   * being analyzed.
   */
  isAnalyzing(
    executionId: string
  ): boolean {

    return this.analyzingExecutionIds.has(
      executionId
    );
  }

  /**
   * Returns the persisted AI analysis
   * for an execution.
   */
  getAnalysis(
    executionId: string
  ): ExecutionAnalysisResponse | null {

    return this.executionAnalyses[
      executionId
    ] ?? null;
  }

  /**
   * Used by the template to determine
   * whether the Analyze button should appear.
   */
  shouldShowAnalyzeButton(
    execution: TaskExecutionResponse
  ): boolean {

    return (
      execution.status === 'COMPLETED' &&
      !this.hasAnalysis(execution.id) &&
      this.executionsWithoutAnalysis.has(
        execution.id
      )
    );
  }
}