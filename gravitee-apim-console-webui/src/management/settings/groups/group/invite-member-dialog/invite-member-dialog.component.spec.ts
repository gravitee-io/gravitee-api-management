import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InviteMemberDialogComponent } from './invite-member-dialog.component';

describe('InviteMemberDialogComponent', () => {
  let component: InviteMemberDialogComponent;
  let fixture: ComponentFixture<InviteMemberDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InviteMemberDialogComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(InviteMemberDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
