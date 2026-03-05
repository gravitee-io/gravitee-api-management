/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { InteractivityChecker } from '@angular/cdk/a11y';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { EditMemberRoleDialogComponent, EditMemberRoleDialogData } from './edit-member-role-dialog.component';
import { EditMemberRoleDialogHarness } from './edit-member-role-dialog.harness';
import { fakeApplicationRoles } from '../../../../../entities/application-members/application-members.fixture';

const FAKE_ROLES = fakeApplicationRoles();

const DIALOG_DATA: EditMemberRoleDialogData = {
  memberName: 'Admin master',
  currentRole: 'OWNER',
  roles: FAKE_ROLES,
};

@Component({
  selector: 'app-test-host',
  template: `<button (click)="openDialog()">Open</button>`,
  standalone: true,
})
class TestHostComponent {
  result: string | null = null;

  constructor(private readonly matDialog: MatDialog) {}

  openDialog(): void {
    this.matDialog
      .open<EditMemberRoleDialogComponent, EditMemberRoleDialogData, string | null>(EditMemberRoleDialogComponent, {
        id: 'editMemberRoleDialog',
        data: DIALOG_DATA,
      })
      .afterClosed()
      .subscribe(role => (this.result = role ?? null));
  }
}

describe('EditMemberRoleDialogComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let rootHarnessLoader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent, EditMemberRoleDialogComponent, NoopAnimationsModule],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true,
          isTabbable: () => true,
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    rootHarnessLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    fixture.detectChanges();

    const btn: HTMLButtonElement = fixture.debugElement.query(By.css('button')).nativeElement;
    btn.click();
  });

  async function getDialog(): Promise<EditMemberRoleDialogHarness> {
    return rootHarnessLoader.getHarness(EditMemberRoleDialogHarness);
  }

  it('should display member name', async () => {
    const dialog = await getDialog();
    expect(await dialog.getMemberNameText()).toBe('Admin master');
  });

  it('should render all roles as options', async () => {
    const dialog = await getDialog();
    const options = await dialog.getRoleOptions();
    expect(options).toEqual(['PRIMARY_OWNER', 'OWNER', 'VIEWER', 'MEMBER']);
  });

  it('should pre-select the current role', async () => {
    const dialog = await getDialog();
    expect(await dialog.getSelectedRole()).toBe('OWNER');
  });

  it('should disable save when role is unchanged', async () => {
    const dialog = await getDialog();
    expect(await dialog.isSaveDisabled()).toBe(true);
  });

  it('should enable save after selecting a different role', async () => {
    const dialog = await getDialog();
    await dialog.selectRole('VIEWER');
    expect(await dialog.isSaveDisabled()).toBe(false);
  });

  it('should return selected role on save', async () => {
    const dialog = await getDialog();
    await dialog.selectRole('MEMBER');
    await dialog.save();

    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.result).toBe('MEMBER');
  });

  it('should return null on cancel', async () => {
    const dialog = await getDialog();
    await dialog.cancel();

    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.result).toBeNull();
  });
});
