import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CustomUserFieldsDialogComponent } from './custom-user-fields-dialog.component';

describe('CustomUserFieldsDialogComponent', () => {
  let component: CustomUserFieldsDialogComponent;
  let fixture: ComponentFixture<CustomUserFieldsDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomUserFieldsDialogComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CustomUserFieldsDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
