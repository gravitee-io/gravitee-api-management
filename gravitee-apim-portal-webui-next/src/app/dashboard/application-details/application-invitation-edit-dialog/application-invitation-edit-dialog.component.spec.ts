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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import {
  ApplicationInvitationEditDialogComponent,
  ApplicationInvitationEditDialogData,
} from './application-invitation-edit-dialog.component';
import { ApplicationInvitationEditDialogHarness } from './application-invitation-edit-dialog.component.harness';
import { APPLICATION_PRIMARY_OWNER_ROLE_NAME, ApplicationRole } from '../../../../entities/application/application';
import { ConfigService } from '../../../../services/config.service';
import { TESTING_BASE_URL } from '../../../../testing/app-testing.module';

const APPLICATION_ID = 'app-1';
const INVITATION_ID = 'invitation-1';
const ROLES_URL = `${TESTING_BASE_URL}/configuration/applications/roles`;
const INVITATION_URL = `${TESTING_BASE_URL}/applications/${APPLICATION_ID}/invitations/${INVITATION_ID}`;

const DIALOG_DATA: ApplicationInvitationEditDialogData = {
  applicationId: APPLICATION_ID,
  invitationId: INVITATION_ID,
  invitationEmail: 'alice@example.com',
  currentRole: 'USER',
};

const APPLICATION_ROLES: ApplicationRole[] = [
  { id: APPLICATION_PRIMARY_OWNER_ROLE_NAME, name: APPLICATION_PRIMARY_OWNER_ROLE_NAME, default: false, system: true },
  { id: 'EMPTY', name: '', default: false, system: false },
  { id: 'OWNER', name: 'OWNER', default: false, system: false },
  { id: 'USER', name: 'USER', default: true, system: false },
];

describe('ApplicationInvitationEditDialogComponent', () => {
  let fixture: ComponentFixture<ApplicationInvitationEditDialogComponent>;
  let harness: ApplicationInvitationEditDialogHarness;
  let httpTestingController: HttpTestingController;
  let dialogRef: { close: jest.Mock };

  async function init(
    data: ApplicationInvitationEditDialogData = DIALOG_DATA,
    roles: ApplicationRole[] | null = APPLICATION_ROLES,
    createHarness = true,
  ): Promise<void> {
    dialogRef = { close: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [ApplicationInvitationEditDialogComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        { provide: ConfigService, useValue: { baseURL: TESTING_BASE_URL } },
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApplicationInvitationEditDialogComponent);
    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
    if (roles) {
      flushRoles(roles);
      await fixture.whenStable();
      fixture.detectChanges();
    }
    if (createHarness) {
      harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApplicationInvitationEditDialogHarness);
    }
  }

  function flushRoles(roles: ApplicationRole[] = APPLICATION_ROLES): void {
    const req = httpTestingController.expectOne(ROLES_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ data: roles });
  }

  afterEach(() => httpTestingController.verify());

  it('should preselect current role and keep save disabled while unchanged', async () => {
    await init();

    expect(await harness.getRoleValueText()).toBe('USER');
    expect(await harness.isSaveDisabled()).toBe(true);
  });

  it('should offer only assignable roles', async () => {
    await init();

    expect(await harness.getRoleOptionTexts()).toEqual(['OWNER', 'USER']);
  });

  it('should enable save when role changes', async () => {
    await init();

    await harness.selectRole('OWNER');

    expect(await harness.isSaveDisabled()).toBe(false);
  });

  it('should update invitation role and close dialog on success', async () => {
    await init();

    await harness.selectRole('OWNER');
    await harness.clickSave();
    fixture.detectChanges();

    const req = httpTestingController.expectOne(INVITATION_URL);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ role: 'OWNER' });
    req.flush({ id: INVITATION_ID, email: 'alice@example.com', role: 'OWNER' });
    await fixture.whenStable();

    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('should close dialog without update when cancelled', async () => {
    await init();

    await harness.clickCancel();

    expect(dialogRef.close).toHaveBeenCalledWith(false);
    httpTestingController.expectNone(INVITATION_URL);
  });

  it('should keep dialog open and show inline error when update fails', async () => {
    await init();

    await harness.selectRole('OWNER');
    await harness.clickSave();
    fixture.detectChanges();

    httpTestingController.expectOne(INVITATION_URL).flush({ message: 'Server error' }, { status: 500, statusText: 'Error' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(await harness.getSubmitErrorText()).toContain('An error occurred while updating the invitation role');
    expect(await harness.isSaveDisabled()).toBe(false);
  });

  it('should show recoverable roles loading error', async () => {
    await init(DIALOG_DATA, null, false);

    httpTestingController.expectOne(ROLES_URL).flush({ message: 'Server error' }, { status: 500, statusText: 'Error' });
    await fixture.whenStable();
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApplicationInvitationEditDialogHarness);

    expect(await harness.getRolesErrorText()).toContain('An error occurred while loading application roles');
  });
});
