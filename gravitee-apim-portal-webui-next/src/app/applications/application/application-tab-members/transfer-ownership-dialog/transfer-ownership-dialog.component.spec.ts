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
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { By } from '@angular/platform-browser';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';

import { TransferOwnershipDialogComponent, TransferOwnershipDialogData } from './transfer-ownership-dialog.component';
import { TransferOwnershipDialogHarness } from './transfer-ownership-dialog.harness';
import { TransferOwnershipRequest } from '../../../../../entities/application-members/application-members';
import { fakeApplicationRoles, fakeMember } from '../../../../../entities/application-members/application-members.fixture';

const FAKE_MEMBERS = [
  fakeMember(),
  fakeMember({ id: 'member-2', user: { id: 'user-2', display_name: 'Doe', email: 'doe@co.com' }, role: 'VIEWER' }),
  fakeMember({ id: 'member-3', user: { id: 'user-3', display_name: 'Jane', email: 'jane@co.com' }, role: 'OWNER' }),
];
const FAKE_ROLES = fakeApplicationRoles().filter(r => !r.system && r.name !== 'PRIMARY_OWNER');

const DIALOG_DATA: TransferOwnershipDialogData = {
  applicationId: 'app-1',
  members: FAKE_MEMBERS,
  roles: FAKE_ROLES,
};

@Component({
  selector: 'app-test-host',
  template: `<button (click)="openDialog()">Open</button>`,
  standalone: true,
})
class TestHostComponent {
  result: TransferOwnershipRequest | null | undefined = undefined;

  constructor(private readonly matDialog: MatDialog) {}

  openDialog(): void {
    this.matDialog
      .open<TransferOwnershipDialogComponent, TransferOwnershipDialogData, TransferOwnershipRequest | null>(
        TransferOwnershipDialogComponent,
        { id: 'transferOwnershipDialog', data: DIALOG_DATA },
      )
      .afterClosed()
      .subscribe(r => (this.result = r ?? null));
  }
}

describe('TransferOwnershipDialogComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let rootHarnessLoader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent, TransferOwnershipDialogComponent],
      providers: [provideNoopAnimations(), provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: { isFocusable: () => true, isTabbable: () => true },
      })
      .compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    rootHarnessLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    fixture.detectChanges();

    const btn: HTMLButtonElement = fixture.debugElement.query(By.css('button')).nativeElement;
    btn.click();
  });

  async function getDialog(): Promise<TransferOwnershipDialogHarness> {
    return rootHarnessLoader.getHarness(TransferOwnershipDialogHarness);
  }

  it('should default to "Application member" mode', async () => {
    const dialog = await getDialog();
    const mode = await dialog.getActiveMode();
    expect(mode).toBe('Application member');
  });

  it('should disable Transfer button when no member selected', async () => {
    const dialog = await getDialog();
    expect(await dialog.isTransferDisabled()).toBe(true);
  });

  it('should enable Transfer button after selecting a member', async () => {
    const dialog = await getDialog();
    await dialog.selectMember('Doe');
    expect(await dialog.isTransferDisabled()).toBe(false);
  });

  it('should show warning when "Primary owner group" is selected', async () => {
    const dialog = await getDialog();
    await dialog.selectMode('Primary owner group');
    expect(await dialog.isWarningVisible()).toBe(true);
  });

  it('should not show warning in "Application member" mode', async () => {
    const dialog = await getDialog();
    expect(await dialog.isWarningVisible()).toBe(false);
  });

  it('should return correct payload on transfer with member mode', async () => {
    const dialog = await getDialog();
    await dialog.selectMember('Doe');
    await dialog.transfer();

    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.result).toEqual({
      newOwnerId: 'member-2',
      newOwnerReference: 'member',
      previousOwnerNewRole: 'VIEWER',
    });
  });

  it('should return null on cancel', async () => {
    const dialog = await getDialog();
    await dialog.cancel();

    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.result).toBeNull();
  });
});
