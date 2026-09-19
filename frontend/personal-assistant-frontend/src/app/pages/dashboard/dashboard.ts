import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

interface DashboardTask {
  title: string;
  description: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  time: string;
  status: 'TODO' | 'IN_PROGRESS' | 'COMPLETED';
}

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard {

  readonly stats = [
    {
      label: 'Tasks Today',
      value: 5,
      type: 'primary'
    },
    {
      label: 'In Progress',
      value: 2,
      type: 'warning'
    },
    {
      label: 'Completed',
      value: 1,
      type: 'success'
    },
    {
      label: 'Upcoming',
      value: 3,
      type: 'danger'
    }
  ];

  readonly todaysTasks: DashboardTask[] = [
    {
      title: 'Finish Angular component',
      description: 'Implement the capture page UI and API integration.',
      priority: 'HIGH',
      time: '10:00 AM',
      status: 'IN_PROGRESS'
    },
    {
      title: 'Gym workout',
      description: 'Upper body workout.',
      priority: 'MEDIUM',
      time: '6:00 PM',
      status: 'TODO'
    },
    {
      title: 'Read system design notes',
      description: 'Review caching and database indexing.',
      priority: 'LOW',
      time: '9:00 PM',
      status: 'TODO'
    }
  ];

  readonly upcomingTasks = [
    {
      title: 'Buy groceries',
      time: 'Tomorrow',
      priority: 'MEDIUM'
    },
    {
      title: 'Plan weekend trip',
      time: 'This weekend',
      priority: 'LOW'
    }
  ];
}