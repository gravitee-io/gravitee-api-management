import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ApiScoreComponent } from './api-score.component';

describe('ApiScoreComponent', () => {
  let component: ApiScoreComponent;
  let fixture: ComponentFixture<ApiScoreComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiScoreComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ApiScoreComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
