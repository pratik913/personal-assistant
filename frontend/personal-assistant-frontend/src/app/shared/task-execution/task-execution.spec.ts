import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TaskExecution } from './task-execution';

describe('TaskExecution', () => {
  let component: TaskExecution;
  let fixture: ComponentFixture<TaskExecution>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaskExecution]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TaskExecution);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
