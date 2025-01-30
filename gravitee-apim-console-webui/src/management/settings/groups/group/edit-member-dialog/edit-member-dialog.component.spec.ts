import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditMemberDialogComponent } from './edit-member-dialog.component';

describe('EditMemberDialogComponent', () => {
  let component: EditMemberDialogComponent;
  let fixture: ComponentFixture<EditMemberDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditMemberDialogComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(EditMemberDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
