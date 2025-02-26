import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RuntimeAlertCreateNotificationsComponent } from './runtime-alert-create-notifications.component';

describe('RuntimeAlertCreateNotificationsComponent', () => {
  let component: RuntimeAlertCreateNotificationsComponent;
  let fixture: ComponentFixture<RuntimeAlertCreateNotificationsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RuntimeAlertCreateNotificationsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RuntimeAlertCreateNotificationsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
