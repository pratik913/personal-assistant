import {
  ChangeDetectorRef,
  Component,
  OnInit
} from '@angular/core';

import {
  RouterLink
} from '@angular/router';

import {
  TaskResponse,
  TaskService
} from '../../services/task.service';

import {
  ScheduleEntryResponse,
  ScheduleService
} from '../../services/schedule.service';

import {
  UserProfileResponse,
  UserProfileService
} from '../../services/user-profile.service';

interface DashboardTask {
  id: string;
  title: string;
  description: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  time: string;
  status:
    | 'TODO'
    | 'IN_PROGRESS'
    | 'COMPLETED'
    | 'CANCELLED';
}

interface UpcomingTask {
  id: string;
  title: string;
  time: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard implements OnInit {

  userName = 'there';

  currentDate = new Date();

  isLoading = true;

  errorMessage = '';

  tasks: TaskResponse[] = [];

  scheduleEntries: ScheduleEntryResponse[] = [];

  stats = [
    {
      label: 'Tasks Today',
      value: 0,
      type: 'primary'
    },
    {
      label: 'In Progress',
      value: 0,
      type: 'warning'
    },
    {
      label: 'Completed',
      value: 0,
      type: 'success'
    },
    {
      label: 'Upcoming',
      value: 0,
      type: 'danger'
    }
  ];

  todaysTasks: DashboardTask[] = [];

  upcomingTasks: UpcomingTask[] = [];

  constructor(
    private readonly taskService: TaskService,
    private readonly scheduleService: ScheduleService,
    private readonly userProfileService: UserProfileService,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {}

  ngOnInit(): void {

    this.loadDashboard();
  }

  get greeting(): string {

    const hour =
      this.currentDate.getHours();

    if (hour < 12) {
      return 'Good morning';
    }

    if (hour < 17) {
      return 'Good afternoon';
    }

    return 'Good evening';
  }

  get formattedWeekday(): string {

    return this.currentDate.toLocaleDateString(
      'en-IN',
      {
        weekday: 'long'
      }
    );
  }

  get formattedDate(): string {

    return this.currentDate.toLocaleDateString(
      'en-IN',
      {
        month: 'short',
        day: 'numeric',
        year: 'numeric'
      }
    );
  }

  private loadDashboard(): void {

    this.isLoading = true;

    this.errorMessage = '';

    this.loadProfile();

    this.loadTasks();

    this.loadSchedule();
  }

  private loadProfile(): void {

    this.userProfileService
      .getProfile()
      .subscribe({

        next: (
          profile: UserProfileResponse
        ) => {

          this.userName =
            profile.name?.trim() || 'there';

          this.changeDetectorRef.detectChanges();
        },

        error: error => {

          console.error(
            '[Dashboard] Profile load failed:',
            error
          );

          /*
           * Profile failure should not prevent
           * task data from loading.
           */
        }
      });
  }

  private loadTasks(): void {

    this.taskService
      .getTasks()
      .subscribe({

        next: tasks => {

          console.log(
            '[Dashboard] Tasks received:',
            tasks
          );

          this.tasks = tasks;

          this.buildDashboardData();

          this.changeDetectorRef.detectChanges();
        },

        error: error => {

          console.error(
            '[Dashboard] Task load failed:',
            error
          );

          this.errorMessage =
            'Unable to load your tasks.';

          this.isLoading = false;

          this.changeDetectorRef.detectChanges();
        }
      });
  }

  private loadSchedule(): void {

    this.scheduleService
      .getScheduleEntries()
      .subscribe({

        next: entries => {

          console.log(
            '[Dashboard] Schedule received:',
            entries
          );

          this.scheduleEntries = entries;

          this.buildDashboardData();

          this.isLoading = false;

          this.changeDetectorRef.detectChanges();
        },

        error: error => {

          console.error(
            '[Dashboard] Schedule load failed:',
            error
          );

          /*
           * Dashboard can still work using tasks
           * even if schedule loading fails.
           */
          this.scheduleEntries = [];

          this.buildDashboardData();

          this.isLoading = false;

          this.changeDetectorRef.detectChanges();
        }
      });
  }

  private buildDashboardData(): void {

    const today =
      this.formatDate(
        new Date()
      );

    const todaySchedule =
      this.scheduleEntries
        .filter(entry => {

          const start =
            new Date(entry.startAt);

          return (
            this.formatDate(start) === today
          );
        })
        .sort(
          (a, b) =>
            new Date(a.startAt).getTime() -
            new Date(b.startAt).getTime()
        );

    const todayTaskIds =
      new Set<string>();

    const dashboardTasks: DashboardTask[] = [];

    /*
     * First show tasks that are actually scheduled today.
     */
    for (
      const entry of todaySchedule
    ) {

      const task =
        this.tasks.find(
          item =>
            item.id === entry.taskId
        );

      if (!task) {
        continue;
      }

      if (
        task.status === 'CANCELLED'
      ) {
        continue;
      }

      todayTaskIds.add(task.id);

      dashboardTasks.push({
        id: task.id,
        title: task.title,
        description:
          task.description ||
          'No description provided.',
        priority: task.priority,
        time: this.formatScheduleTime(
          entry.startAt
        ),
        status: task.status
      });
    }

    /*
     * Also show tasks whose due date is today,
     * even if they have not been scheduled.
     */
    const dueToday =
      this.tasks.filter(task => {

        return (
          task.status !== 'CANCELLED' &&
          task.dueDate === today &&
          !todayTaskIds.has(task.id)
        );
      });

    for (
      const task of dueToday
    ) {

      dashboardTasks.push({
        id: task.id,
        title: task.title,
        description:
          task.description ||
          'No description provided.',
        priority: task.priority,
        time: 'Due today',
        status: task.status
      });
    }

    /*
     * If there is no explicit schedule/due date,
     * still show the user's active tasks rather than
     * displaying a misleading empty dashboard.
     */
    if (
      dashboardTasks.length === 0
    ) {

      const activeTasks =
        this.tasks
          .filter(
            task =>
              task.status !== 'COMPLETED' &&
              task.status !== 'CANCELLED'
          )
          .sort(
            (a, b) =>
              this.priorityRank(
                b.priority
              ) -
              this.priorityRank(
                a.priority
              )
          )
          .slice(0, 5);

      for (
        const task of activeTasks
      ) {

        dashboardTasks.push({
          id: task.id,
          title: task.title,
          description:
            task.description ||
            'No description provided.',
          priority: task.priority,
          time: 'Not scheduled',
          status: task.status
        });
      }
    }

    this.todaysTasks =
      dashboardTasks.slice(0, 5);

    const inProgressCount =
      this.tasks.filter(
        task =>
          task.status === 'IN_PROGRESS'
      ).length;

    const completedCount =
      this.tasks.filter(
        task =>
          task.status === 'COMPLETED'
      ).length;

    const upcomingCount =
      this.scheduleEntries.filter(
        entry =>
          new Date(entry.startAt) >
          new Date()
      ).length;

    this.stats = [
      {
        label: 'Tasks Today',
        value: this.todaysTasks.length,
        type: 'primary'
      },
      {
        label: 'In Progress',
        value: inProgressCount,
        type: 'warning'
      },
      {
        label: 'Completed',
        value: completedCount,
        type: 'success'
      },
      {
        label: 'Upcoming',
        value: upcomingCount,
        type: 'danger'
      }
    ];

    this.buildUpcomingTasks();
  }

  private buildUpcomingTasks(): void {

    const now =
      new Date();

    this.upcomingTasks =
      this.scheduleEntries
        .filter(
          entry =>
            new Date(entry.startAt) > now
        )
        .sort(
          (a, b) =>
            new Date(a.startAt).getTime() -
            new Date(b.startAt).getTime()
        )
        .slice(0, 5)
        .map(entry => {

          const task =
            this.tasks.find(
              item =>
                item.id === entry.taskId
            );

          return {
            id: entry.taskId,
            title: entry.taskTitle,
            time: this.formatUpcomingTime(
              entry.startAt
            ),
            priority:
              task?.priority || 'MEDIUM'
          };
        });
  }

  private priorityRank(
    priority: 'HIGH' | 'MEDIUM' | 'LOW'
  ): number {

    if (priority === 'HIGH') {
      return 3;
    }

    if (priority === 'MEDIUM') {
      return 2;
    }

    return 1;
  }

  private formatScheduleTime(
    value: string
  ): string {

    return new Date(value)
      .toLocaleTimeString(
        'en-IN',
        {
          hour: 'numeric',
          minute: '2-digit',
          hour12: true
        }
      );
  }

  private formatUpcomingTime(
    value: string
  ): string {

    const date =
      new Date(value);

    const today =
      this.formatDate(
        new Date()
      );

    const tomorrowDate =
      new Date();

    tomorrowDate.setDate(
      tomorrowDate.getDate() + 1
    );

    const tomorrow =
      this.formatDate(
        tomorrowDate
      );

    const dateValue =
      this.formatDate(date);

    const time =
      this.formatScheduleTime(value);

    if (dateValue === today) {
      return `Today · ${time}`;
    }

    if (dateValue === tomorrow) {
      return `Tomorrow · ${time}`;
    }

    return `${date.toLocaleDateString(
      'en-IN',
      {
        day: 'numeric',
        month: 'short'
      }
    )} · ${time}`;
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
}