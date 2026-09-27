import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-landing',
  standalone: true,
  templateUrl: './landing.html',
  styleUrl: './landing.scss'
})
export class LandingComponent {

  constructor(
    private readonly router: Router
  ) {}

  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  goToRegister(): void {
    this.router.navigate(['/register']);
  }

  scrollToFeatures(): void {
    document.getElementById('features')
      ?.scrollIntoView({
        behavior: 'smooth'
      });
  }

  scrollToHowItWorks(): void {
    document.getElementById('how-it-works')
      ?.scrollIntoView({
        behavior: 'smooth'
      });
  }
}