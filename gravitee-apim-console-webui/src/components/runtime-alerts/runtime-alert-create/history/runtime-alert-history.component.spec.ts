import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RuntimeAlertHistoryComponent } from './runtime-alert-history.component';

describe('RuntimeAlertHistoryComponent', () => {
  let component: RuntimeAlertHistoryComponent;
  let fixture: ComponentFixture<RuntimeAlertHistoryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RuntimeAlertHistoryComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RuntimeAlertHistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
