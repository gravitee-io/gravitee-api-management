import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AnalyticsOverviewComponent } from './analytics-overview.component';

describe('AnalyticsOverviewComponent', () => {
  let component: AnalyticsOverviewComponent;
  let fixture: ComponentFixture<AnalyticsOverviewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnalyticsOverviewComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AnalyticsOverviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
