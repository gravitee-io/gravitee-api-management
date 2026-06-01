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
  ApplicationTransferOwnershipDialogComponent,
  ApplicationTransferOwnershipDialogData,
} from './application-transfer-ownership-dialog.component';
import { ApplicationTransferOwnershipDialogHarness } from './application-transfer-ownership-dialog.component.harness';
import { APPLICATION_PRIMARY_OWNER_ROLE_NAME, ApplicationRole } from '../../../../entities/application/application';
import { MembersResponse } from '../../../../entities/member/member';
import { fakeMember, fakeMembersResponse } from '../../../../entities/member/member.fixtures';
import { User, UsersResponse } from '../../../../entities/user/user';
import { fakeUser, fakeUsersResponse } from '../../../../entities/user/user.fixtures';
import { ConfigService } from '../../../../services/config.service';
import { TESTING_BASE_URL } from '../../../../testing/app-testing.module';

const APPLICATION_ID = 'app-1';
const OWNER_ID = 'owner-1';
const ROLES_URL = `${TESTING_BASE_URL}/configuration/applications/roles`;
const TRANSFER_URL = `${TESTING_BASE_URL}/applications/${APPLICATION_ID}/members/_transfer_ownership`;
const MEMBERS_SEARCH_URL = `${TESTING_BASE_URL}/applications/${APPLICATION_ID}/members/_search`;
const USERS_SEARCH_URL = `${TESTING_BASE_URL}/users/_search`;

const DIALOG_DATA: ApplicationTransferOwnershipDialogData = {
  applicationId: APPLICATION_ID,
  currentOwnerId: OWNER_ID,
};

const APPLICATION_MEMBERS_RESPONSE = fakeMembersResponse([
  fakeMember({
    id: OWNER_ID,
    role: APPLICATION_PRIMARY_OWNER_ROLE_NAME,
    user: { id: OWNER_ID, display_name: 'Current Owner', _links: {} },
  }),
  fakeMember({
    id: 'member-1',
    user: { id: 'member-1', reference: 'member-reference', display_name: 'Alice Smith', email: 'alice@example.com', _links: {} },
  }),
]);

const APPLICATION_ROLES: ApplicationRole[] = [
  { id: APPLICATION_PRIMARY_OWNER_ROLE_NAME, name: APPLICATION_PRIMARY_OWNER_ROLE_NAME, default: false, system: true },
  { id: 'SYSTEM_AUDITOR', name: 'SYSTEM_AUDITOR', default: false, system: true },
  { id: 'EMPTY', name: '', default: false, system: false },
  { id: 'OWNER', name: 'OWNER', default: false, system: false },
  { id: 'USER', name: 'USER', default: true, system: false },
];

describe('ApplicationTransferOwnershipDialogComponent', () => {
  let fixture: ComponentFixture<ApplicationTransferOwnershipDialogComponent>;
  let harness: ApplicationTransferOwnershipDialogHarness;
  let httpTestingController: HttpTestingController;
  let dialogRef: { close: jest.Mock };

  async function init(
    data: ApplicationTransferOwnershipDialogData = DIALOG_DATA,
    roles: ApplicationRole[] | null = APPLICATION_ROLES,
    applicationMembers: MembersResponse | null = null,
    createHarness = true,
  ): Promise<void> {
    jest.useRealTimers();
    dialogRef = { close: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [ApplicationTransferOwnershipDialogComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        { provide: ConfigService, useValue: { baseURL: TESTING_BASE_URL } },
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApplicationTransferOwnershipDialogComponent);
    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
    if (roles) {
      flushRoles(roles);
    }
    if (applicationMembers) {
      flushApplicationMembers(applicationMembers);
    }
    if (roles || applicationMembers) {
      await fixture.whenStable();
      fixture.detectChanges();
    }
    if (createHarness) {
      harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApplicationTransferOwnershipDialogHarness);
    }
  }

  function flushRoles(roles: ApplicationRole[] = APPLICATION_ROLES): void {
    const req = httpTestingController.expectOne(ROLES_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ data: roles });
  }

  function flushApplicationMembers(response: MembersResponse = APPLICATION_MEMBERS_RESPONSE, query = ''): void {
    const req = httpTestingController.expectOne(req => req.url === MEMBERS_SEARCH_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('20');
    expect(req.request.body).toEqual({
      filters: {
        displayName: query,
      },
    });
    req.flush(response);
  }

  async function searchApplicationMember(query: string, response: MembersResponse): Promise<void> {
    await harness.searchApplicationMember(query);
    fixture.detectChanges();
    await new Promise(resolve => setTimeout(resolve, 350));
    fixture.detectChanges();

    flushApplicationMembers(response, query);
    await fixture.whenStable();
    fixture.detectChanges();
  }

  async function searchOtherUser(query: string, response: UsersResponse): Promise<void> {
    await harness.selectMethod(/Other user/);
    fixture.detectChanges();
    await harness.searchOtherUser(query);
    fixture.detectChanges();
    await new Promise(resolve => setTimeout(resolve, 350));
    fixture.detectChanges();

    const req = httpTestingController.expectOne(req => req.url === USERS_SEARCH_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('20');
    expect(req.request.body).toEqual({
      filters: {
        query,
      },
      includes: {
        applicationMembership: APPLICATION_ID,
      },
    });
    req.flush(response);
    await fixture.whenStable();
    fixture.detectChanges();
  }

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should show only user transfer methods', async () => {
    await init();

    const methodTexts = await harness.getMethodOptionTexts();
    expect(methodTexts).toEqual([expect.stringContaining('Application member'), expect.stringContaining('Other user')]);
    expect(methodTexts.some(text => text.includes('Primary owner group'))).toBe(false);
  });

  it('should not search application members before query is entered', async () => {
    await init();

    httpTestingController.expectNone(req => req.url === MEMBERS_SEARCH_URL);
  });

  it('should offer only assignable roles for the current owner new role', async () => {
    await init();

    expect(await harness.getRoleOptionTexts()).toEqual(['OWNER', 'USER']);
  });

  it('should keep transfer disabled until target and role are selected', async () => {
    await init();

    await searchApplicationMember('Alice', fakeMembersResponse([aliceMember()]));
    await harness.selectApplicationMember(/Alice Smith/);
    fixture.detectChanges();

    expect(await harness.isSubmitDisabled()).toBe(true);

    await harness.selectRole('OWNER');

    expect(await harness.isSubmitDisabled()).toBe(false);
  });

  it('should transfer ownership to an application member', async () => {
    await init();

    await searchApplicationMember('Alice', fakeMembersResponse([aliceMember()]));
    expect(await harness.getApplicationMemberOptionTexts()).toEqual([expect.stringContaining('Alice Smith')]);
    await harness.selectApplicationMember(/Alice Smith/);
    await harness.selectRole('OWNER');
    await harness.clickSubmit();
    fixture.detectChanges();

    const req = httpTestingController.expectOne(TRANSFER_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      new_primary_owner_id: 'member-1',
      new_primary_owner_reference: 'member-reference',
      primary_owner_newrole: 'OWNER',
    });
    req.flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();

    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('should show the selected application member email after selection', async () => {
    await init();

    await searchApplicationMember('Alice', fakeMembersResponse([aliceMember()]));
    await harness.selectApplicationMember(/Alice Smith/);
    fixture.detectChanges();

    expect(await harness.getSelectedTargetText()).toContain('alice@example.com');
  });

  it('should search application members independently from the members table page', async () => {
    await init();

    await searchApplicationMember(
      'Page Two',
      fakeMembersResponse([
        fakeMember({
          id: 'page-2-member',
          user: {
            id: 'page-2-member',
            reference: 'page-2-reference',
            display_name: 'Page Two Member',
            email: 'page.two@example.com',
            _links: {},
          },
        }),
      ]),
    );
    await harness.selectApplicationMember(/Page Two Member/);
    await harness.selectRole('OWNER');
    await harness.clickSubmit();
    fixture.detectChanges();

    const req = httpTestingController.expectOne(TRANSFER_URL);
    expect(req.request.body).toEqual({
      new_primary_owner_id: 'page-2-member',
      new_primary_owner_reference: 'page-2-reference',
      primary_owner_newrole: 'OWNER',
    });
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('should transfer ownership to another registered user', async () => {
    const user = fakeUser({ id: 'user-2', reference: 'user-reference', display_name: 'Bob Martin', email: 'bob@example.com' });
    await init();

    await searchOtherUser('Bob', usersResponse([user]));
    expect(await harness.getOtherUserOptionTexts()).toEqual([expect.stringContaining('Bob Martin')]);
    await harness.selectOtherUser(/Bob Martin/);
    await harness.selectRole('USER');
    await harness.clickSubmit();
    fixture.detectChanges();

    const req = httpTestingController.expectOne(TRANSFER_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      new_primary_owner_id: 'user-2',
      new_primary_owner_reference: 'user-reference',
      primary_owner_newrole: 'USER',
    });
    req.flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();

    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('should annotate already assigned users without blocking transfer in other user search', async () => {
    const user = fakeUser({ id: 'user-1', display_name: 'Ada Lovelace' });
    await init();

    await searchOtherUser('Ada', usersResponse([user], { 'user-1': true }));

    expect(await harness.getOtherUserOptionTexts()).toEqual([expect.stringContaining('Already member')]);
    expect(await harness.isOtherUserOptionDisabled(/Ada Lovelace/)).toBe(false);
  });

  it('should keep dialog open and show inline error when transfer fails', async () => {
    await init();

    await searchApplicationMember('Alice', fakeMembersResponse([aliceMember()]));
    await harness.selectApplicationMember(/Alice Smith/);
    await harness.selectRole('OWNER');
    await harness.clickSubmit();
    fixture.detectChanges();

    httpTestingController.expectOne(TRANSFER_URL).flush({ message: 'Server error' }, { status: 500, statusText: 'Error' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(await harness.getSubmitErrorText()).toContain('An error occurred while transferring ownership');
    expect(await harness.isSubmitDisabled()).toBe(false);
  });

  it('should close dialog without transfer when cancelled', async () => {
    await init();

    await harness.clickCancel();

    expect(dialogRef.close).toHaveBeenCalledWith(false);
    httpTestingController.expectNone(TRANSFER_URL);
  });

  it('should show recoverable roles loading error', async () => {
    await init(DIALOG_DATA, null, null, false);

    httpTestingController.expectOne(ROLES_URL).flush({ message: 'Server error' }, { status: 500, statusText: 'Error' });
    await fixture.whenStable();
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApplicationTransferOwnershipDialogHarness);

    expect(await harness.getRolesErrorText()).toContain('An error occurred while loading application roles');
  });
});

function usersResponse(users: User[], applicationMembership: Record<string, boolean> = {}): UsersResponse {
  return fakeUsersResponse({
    data: users,
    metadata: {
      data: { total: users.length },
      applicationMembership,
    },
  });
}

function aliceMember() {
  return fakeMember({
    id: 'member-1',
    user: { id: 'member-1', reference: 'member-reference', display_name: 'Alice Smith', email: 'alice@example.com', _links: {} },
  });
}
