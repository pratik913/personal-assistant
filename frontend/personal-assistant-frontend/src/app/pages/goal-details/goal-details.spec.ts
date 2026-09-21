import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GoalDetails } from './goal-details';

describe('GoalDetails', () => {
  let component: GoalDetails;
  let fixture: ComponentFixture<GoalDetails>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GoalDetails]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GoalDetails);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
