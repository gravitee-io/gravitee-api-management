import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ApiHistoryV4DeploymentCompareComponent } from './api-history-v4-deployment-compare.component';

describe('DeploymentCompareCurrentComponent', () => {
  let component: ApiHistoryV4DeploymentCompareComponent;
  let fixture: ComponentFixture<ApiHistoryV4DeploymentCompareComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ApiHistoryV4DeploymentCompareComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiHistoryV4DeploymentCompareComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
