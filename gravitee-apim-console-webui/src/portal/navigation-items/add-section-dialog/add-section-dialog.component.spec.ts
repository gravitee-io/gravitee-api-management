import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AddSectionDialogComponent } from './add-section-dialog.component';

describe('AddSectionDialogComponent', () => {
  let component: AddSectionDialogComponent;
  let fixture: ComponentFixture<AddSectionDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AddSectionDialogComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AddSectionDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
