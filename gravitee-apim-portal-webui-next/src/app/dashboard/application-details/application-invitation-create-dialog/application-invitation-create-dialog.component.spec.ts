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
  ApplicationInvitationCreateDialogComponent,
  ApplicationInvitationCreateDialogData,
} from './application-invitation-create-dialog.component';
import { ApplicationInvitationCreateDialogHarness } from './application-invitation-create-dialog.component.harness';
import { APPLICATION_PRIMARY_OWNER_ROLE_NAME, ApplicationRole } from '../../../../entities/application/application';
import { fakeApplicationInvitationsResponse } from '../../../../entities/application/application-invitation.fixture';
import { ConfigService } from '../../../../services/config.service';
import { TESTING_BASE_URL } from '../../../../testing/app-testing.module';

const APPLICATION_ID = 'app-1';
const ROLES_URL = `${TESTING_BASE_URL}/configuration/applications/roles`;
const INVITATIONS_URL = `${TESTING_BASE_URL}/applications/${APPLICATION_ID}/invitations`;

const APPLICATION_ROLES: ApplicationRole[] = [
  { id: APPLICATION_PRIMARY_OWNER_ROLE_NAME, name: APPLICATION_PRIMARY_OWNER_ROLE_NAME, default: false, system: true },
  { id: 'SYSTEM_AUDITOR', name: 'SYSTEM_AUDITOR', default: false, system: true },
  { id: 'EMPTY', name: '', default: false, system: false },
  { id: 'OWNER', name: 'OWNER', default: false, system: false },
  { id: 'USER', name: 'USER', default: true, system: false },
];

describe('ApplicationInvitationCreateDialogComponent', () => {
  let fixture: ComponentFixture<ApplicationInvitationCreateDialogComponent>;
  let harness: ApplicationInvitationCreateDialogHarness;
  let httpTestingController: HttpTestingController;
  let dialogRef: { close: jest.Mock };

  async function init(
    data: ApplicationInvitationCreateDialogData = { applicationId: APPLICATION_ID },
    options: { flushRolesOnInit: boolean } = { flushRolesOnInit: true },
  ): Promise<void> {
    dialogRef = { close: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [ApplicationInvitationCreateDialogComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        { provide: ConfigService, useValue: { baseURL: TESTING_BASE_URL } },
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApplicationInvitationCreateDialogComponent);
    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
    if (options.flushRolesOnInit) {
      flushRoles();
      await fixture.whenStable();
      fixture.detectChanges();
      harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApplicationInvitationCreateDialogHarness);
    }
  }

  function flushRoles(roles: ApplicationRole[] = APPLICATION_ROLES): void {
    const req = httpTestingController.expectOne(ROLES_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ data: roles });
  }

  async function enterEmail(email: string): Promise<void> {
    await harness.enterEmail(email);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should preselect default application role', async () => {
    await init();

    expect(fixture.componentInstance.roleControl.value).toBe('USER');
    expect(await harness.getRoleValueText()).toBe('USER');
  });

  it('should offer only assignable application roles', async () => {
    await init();

    expect(await harness.getRoleOptionTexts()).toEqual(['OWNER', 'USER']);
  });

  it('should show roles loading error when application roles cannot be loaded', async () => {
    await init({ applicationId: APPLICATION_ID }, { flushRolesOnInit: false });

    httpTestingController.expectOne(ROLES_URL).flush({ message: 'error' }, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApplicationInvitationCreateDialogHarness);

    expect(await harness.getRolesErrorText()).toContain('An error occurred while loading application roles');
  });

  it('should add normalized selected email chips and allow removing them', async () => {
    await init();

    await enterEmail(' Alice@Example.com ');
    await enterEmail('bob@example.com');

    expect(await harness.getSelectedEmailChipTexts()).toEqual([
      expect.stringContaining('alice@example.com'),
      expect.stringContaining('bob@example.com'),
    ]);
    expect(await harness.getSubmitButtonText()).toBe('Send 2 invitations');
    expect(await harness.isSubmitDisabled()).toBe(false);

    await harness.removeSelectedEmailChip(/alice@example.com/);

    expect(await harness.getSelectedEmailChipTexts()).toEqual([expect.stringContaining('bob@example.com')]);
    expect(await harness.getSubmitButtonText()).toBe('Send invitation');
  });

  it('should enable notify checked by default when backend notifications are available', async () => {
    await init();

    expect(await harness.isNotifyChecked()).toBe(true);
    expect(await harness.isNotifyDisabled()).toBe(false);
  });

  it('should reject invalid email address', async () => {
    await init();

    await enterEmail('not-an-email');

    expect(await harness.getEmailErrorText()).toContain('Enter a valid email address');
    expect(await harness.getSelectedEmailChipTexts()).toEqual([]);
    expect(await harness.isSubmitDisabled()).toBe(true);
    httpTestingController.expectNone(INVITATIONS_URL);
  });

  it('should not submit when pending email is invalid', async () => {
    await init();
    await enterEmail('alice@example.com');
    fixture.componentInstance.emailControl.setValue('not-an-email');
    fixture.detectChanges();

    fixture.componentInstance.onSubmit();
    fixture.detectChanges();

    expect(await harness.getEmailErrorText()).toContain('Enter a valid email address');
    expect(dialogRef.close).not.toHaveBeenCalled();
    httpTestingController.expectNone(INVITATIONS_URL);
  });

  it('should reject duplicate email address', async () => {
    await init();

    await enterEmail('alice@example.com');
    await enterEmail(' Alice@Example.com ');

    expect(await harness.getEmailErrorText()).toContain('already selected');
    expect(await harness.getSelectedEmailChipTexts()).toEqual([expect.stringContaining('alice@example.com')]);
  });

  it('should create invitations and close dialog on success', async () => {
    await init();
    await enterEmail('alice@example.com');
    await enterEmail('bob@example.com');

    await harness.clickSubmit();
    fixture.detectChanges();

    const request = httpTestingController.expectOne(INVITATIONS_URL);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      recipients: [{ email: 'alice@example.com' }, { email: 'bob@example.com' }],
      role: 'USER',
      notify: true,
      confirmation_page_url: `${globalThis.location.origin}/user/invitation/confirm`,
    });
    request.flush(fakeApplicationInvitationsResponse());
    await fixture.whenStable();

    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('should submit notify false when checkbox is unchecked', async () => {
    await init();
    await enterEmail('alice@example.com');
    await harness.toggleNotify();

    await harness.clickSubmit();
    fixture.detectChanges();

    const request = httpTestingController.expectOne(INVITATIONS_URL);
    expect(request.request.body).toEqual({
      recipients: [{ email: 'alice@example.com' }],
      role: 'USER',
      notify: false,
      confirmation_page_url: `${globalThis.location.origin}/user/invitation/confirm`,
    });
    request.flush(fakeApplicationInvitationsResponse());
    await fixture.whenStable();

    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('should keep dialog open and show conflict error when backend rejects a pending invitation', async () => {
    await init();
    await enterEmail('alice@example.com');

    await harness.clickSubmit();
    fixture.detectChanges();

    httpTestingController.expectOne(INVITATIONS_URL).flush({ message: 'Already exists' }, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(await harness.getSubmitErrorText()).toContain('already has a pending invitation');
  });

  it('should close dialog with false on cancel', async () => {
    await init();

    await harness.clickCancel();

    expect(dialogRef.close).toHaveBeenCalledWith(false);
  });
});
