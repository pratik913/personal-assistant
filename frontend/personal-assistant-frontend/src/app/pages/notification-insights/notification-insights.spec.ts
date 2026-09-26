import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NotificationInsights } from './notification-insights';

describe('NotificationInsights', () => {
  let component: NotificationInsights;
  let fixture: ComponentFixture<NotificationInsights>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotificationInsights]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NotificationInsights);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
