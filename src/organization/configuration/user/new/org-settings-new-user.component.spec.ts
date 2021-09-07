import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OrgSettingsNewUserComponent } from './org-settings-new-user.component';

describe('OrgSettingsNewUserComponent', () => {
  let component: OrgSettingsNewUserComponent;
  let fixture: ComponentFixture<OrgSettingsNewUserComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [OrgSettingsNewUserComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(OrgSettingsNewUserComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
