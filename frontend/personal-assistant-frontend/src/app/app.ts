import { Component } from '@angular/core';
import { ApiService } from './services/api.service';

@Component({
  selector: 'app-root',
  template: `
    <h1>{{ message }}</h1>
  `
})
export class App {

  message = 'Testing backend...';

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.apiService.testBackend().subscribe({
      next: (response) => {
        console.log('Backend response:', response);
        this.message = response.message ?? response.status;
      },
      error: (error) => {
        console.error('Backend error:', error);
        this.message = 'Backend request failed';
      }
    });
  }
}