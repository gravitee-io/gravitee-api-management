import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GraviteeDashboardComponent } from './gravitee-dashboard.component';

describe('GraviteeDashboardComponent', () => {
  let component: GraviteeDashboardComponent;
  let fixture: ComponentFixture<GraviteeDashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GraviteeDashboardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GraviteeDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
