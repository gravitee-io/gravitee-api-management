import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditMembersDialogComponent } from './edit-members-dialog.component';

describe('EditMembersDialogComponent', () => {
  let component: EditMembersDialogComponent;
  let fixture: ComponentFixture<EditMembersDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditMembersDialogComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(EditMembersDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
