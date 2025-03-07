import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ApplicationLogRequestResponseComponent } from './application-log-request-response.component';

describe('ApplicationLogRequestResponseComponent', () => {
  let component: ApplicationLogRequestResponseComponent;
  let fixture: ComponentFixture<ApplicationLogRequestResponseComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationLogRequestResponseComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ApplicationLogRequestResponseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
