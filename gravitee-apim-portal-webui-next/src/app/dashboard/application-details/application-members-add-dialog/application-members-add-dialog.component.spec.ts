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

import { ApplicationMembersAddDialogComponent, ApplicationMembersAddDialogData } from './application-members-add-dialog.component';
import { ApplicationMembersAddDialogHarness } from './application-members-add-dialog.component.harness';
import { APPLICATION_PRIMARY_OWNER_ROLE_NAME, ApplicationRole } from '../../../../entities/application/application';
import { User, UsersResponse } from '../../../../entities/user/user';
import { fakeUser, fakeUsersResponse } from '../../../../entities/user/user.fixtures';
import { ConfigService } from '../../../../services/config.service';
import { TESTING_BASE_URL } from '../../../../testing/app-testing.module';

const APPLICATION_ID = 'app-1';
const ROLES_URL = `${TESTING_BASE_URL}/configuration/applications/roles`;
const MEMBERS_URL = `${TESTING_BASE_URL}/applications/${APPLICATION_ID}/members`;
const USERS_SEARCH_URL = `${TESTING_BASE_URL}/users/_search`;
const MOCK_DATE = new Date(1466424490000);

const APPLICATION_ROLES: ApplicationRole[] = [
  { id: APPLICATION_PRIMARY_OWNER_ROLE_NAME, name: APPLICATION_PRIMARY_OWNER_ROLE_NAME, default: false, system: true },
  { id: 'SYSTEM_AUDITOR', name: 'SYSTEM_AUDITOR', default: false, system: true },
  { id: 'OWNER', name: 'OWNER', default: false, system: false },
  { id: 'USER', name: 'USER', default: true, system: false },
];

describe('ApplicationMembersAddDialogComponent', () => {
  let fixture: ComponentFixture<ApplicationMembersAddDialogComponent>;
  let harness: ApplicationMembersAddDialogHarness;
  let httpTestingController: HttpTestingController;
  let dialogRef: { close: jest.Mock };

  async function init(data: ApplicationMembersAddDialogData = { applicationId: APPLICATION_ID }): Promise<void> {
    jest.useRealTimers();
    dialogRef = { close: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [ApplicationMembersAddDialogComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        { provide: ConfigService, useValue: { baseURL: TESTING_BASE_URL } },
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApplicationMembersAddDialogComponent);
    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
    flushRoles();
    await fixture.whenStable();
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApplicationMembersAddDialogHarness);
  }

  function flushRoles(roles: ApplicationRole[] = APPLICATION_ROLES): void {
    const req = httpTestingController.expectOne(ROLES_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ data: roles });
  }

  async function searchUsers(query: string, response: UsersResponse): Promise<void> {
    fixture.componentInstance.userControl.setValue(query);
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

  async function selectUser(user: User): Promise<void> {
    const displayName = user.display_name ?? user.id ?? '';
    await searchUsers(displayName, usersResponse([user]));
    await harness.selectUser(new RegExp(displayName));
    fixture.detectChanges();
  }

  afterEach(() => {
    httpTestingController.verify();
    jest.useFakeTimers({ advanceTimers: 1, now: MOCK_DATE });
  });

  it('should preselect default application role', async () => {
    await init();

    expect(fixture.componentInstance.roleControl.value).toBe('USER');
    expect(await harness.getRoleValueText()).toBe('USER');
  });

  it('should not offer system application roles', async () => {
    await init({
      applicationId: APPLICATION_ID,
    });

    expect(await harness.getRoleOptionTexts()).toEqual(['OWNER', 'USER']);
  });

  it('should keep submit disabled without calling users search for blank query', async () => {
    await init();

    httpTestingController.expectNone(USERS_SEARCH_URL);
    expect(await harness.isSubmitDisabled()).toBe(true);
  });

  it('should search users through application-aware users service contract', async () => {
    const user = fakeUser({ id: 'user-1', display_name: 'Ada Lovelace', email: 'ada@example.com' });
    await init();

    await searchUsers('Ada', usersResponse([user]));

    expect(await harness.getUserOptionTexts()).toEqual([expect.stringContaining('Ada Lovelace')]);
  });

  it('should mark already assigned users as not selectable', async () => {
    const user = fakeUser({ id: 'user-1', display_name: 'Ada Lovelace' });
    await init();

    await searchUsers('Ada', usersResponse([user], { 'user-1': true }));

    expect(await harness.getUserOptionTexts()).toEqual([expect.stringContaining('Already added')]);
    expect(await harness.isUserOptionDisabled(/Ada Lovelace/)).toBe(true);
  });

  it('should show no users found option when search has no result', async () => {
    await init();

    await searchUsers('Ada', usersResponse([]));

    expect(await harness.getSearchNoMatchText()).toBe('No users found');
  });

  it('should require selecting a user before submit when default role is available', async () => {
    const user = fakeUser({ id: 'user-1', display_name: 'Ada Lovelace' });
    await init();
    await searchUsers('Ada', usersResponse([user]));

    expect(await harness.isSubmitDisabled()).toBe(true);

    await harness.selectUser(/Ada Lovelace/);
    fixture.detectChanges();

    expect(await harness.getSelectedUserChipTexts()).toEqual([expect.stringContaining('Ada Lovelace')]);
    expect(await harness.getSubmitButtonText()).toBe('Add member');
    expect(await harness.isSubmitDisabled()).toBe(false);
  });

  it('should remove selected users from chips', async () => {
    const user = fakeUser({ id: 'user-1', display_name: 'Ada Lovelace' });
    await init();
    await searchUsers('Ada', usersResponse([user]));
    await harness.selectUser(/Ada Lovelace/);
    await harness.selectRole('USER');
    fixture.detectChanges();

    expect(await harness.isSubmitDisabled()).toBe(false);

    await harness.removeSelectedUserChip(/Ada Lovelace/);
    fixture.detectChanges();

    expect(await harness.getSelectedUserChipTexts()).toEqual([]);
    expect(await harness.isSubmitDisabled()).toBe(true);
  });

  it('should mark selected users as not selectable when they appear again in search results', async () => {
    const user = fakeUser({ id: 'user-1', display_name: 'Ada Lovelace' });
    await init();
    await searchUsers('Ada', usersResponse([user]));
    await harness.selectUser(/Ada Lovelace/);
    fixture.detectChanges();

    await searchUsers('Ada', usersResponse([user]));

    expect(await harness.getUserOptionTexts()).toEqual([expect.stringContaining('Already selected')]);
    expect(await harness.isUserOptionDisabled(/Ada Lovelace/)).toBe(true);
  });

  it('should create selected application members and close dialog on success', async () => {
    const ada = fakeUser({ id: 'user-1', display_name: 'Ada Lovelace' });
    const bob = fakeUser({ id: 'user-2', display_name: 'Bob Martin' });
    await init();
    await searchUsers('Ada', usersResponse([ada]));
    await harness.selectUser(/Ada Lovelace/);
    await searchUsers('Bob', usersResponse([bob]));
    await harness.selectUser(/Bob Martin/);
    await harness.selectRole('USER');
    fixture.detectChanges();

    expect(await harness.getSubmitButtonText()).toBe('Add 2 members');

    fixture.componentInstance.onSubmit();
    fixture.detectChanges();

    const requests = httpTestingController.match(req => req.url === MEMBERS_URL);
    expect(requests).toHaveLength(2);
    expect(requests.map(req => req.request.method)).toEqual(['POST', 'POST']);
    expect(requests.map(req => req.request.body)).toEqual([
      { user: 'user-1', role: 'USER' },
      { user: 'user-2', role: 'USER' },
    ]);
    requests[0].flush({ id: 'member-1', role: 'USER' });
    requests[1].flush({ id: 'member-2', role: 'USER' });
    await fixture.whenStable();

    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('should limit member creation to three concurrent requests', async () => {
    const users = [
      fakeUser({ id: 'user-1', display_name: 'Ada Lovelace' }),
      fakeUser({ id: 'user-2', display_name: 'Bob Martin' }),
      fakeUser({ id: 'user-3', display_name: 'Grace Hopper' }),
      fakeUser({ id: 'user-4', display_name: 'Katherine Johnson' }),
    ];
    await init();
    for (const user of users) {
      await selectUser(user);
    }
    await harness.selectRole('USER');
    fixture.detectChanges();

    fixture.componentInstance.onSubmit();
    fixture.detectChanges();

    const initialRequests = httpTestingController.match(req => req.url === MEMBERS_URL);
    expect(initialRequests).toHaveLength(3);
    expect(initialRequests.map(req => req.request.body.user)).toEqual(['user-1', 'user-2', 'user-3']);

    initialRequests[0].flush({ id: 'member-1', role: 'USER' });

    const fourthRequest = httpTestingController.expectOne(req => req.url === MEMBERS_URL && req.body.user === 'user-4');
    expect(fourthRequest.request.method).toBe('POST');
    expect(fourthRequest.request.body).toEqual({ user: 'user-4', role: 'USER' });

    initialRequests[1].flush({ id: 'member-2', role: 'USER' });
    initialRequests[2].flush({ id: 'member-3', role: 'USER' });
    fourthRequest.flush({ id: 'member-4', role: 'USER' });
    await fixture.whenStable();

    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('should keep dialog open and show conflict error when backend rejects existing member', async () => {
    const user = fakeUser({ id: 'user-1', display_name: 'Ada Lovelace' });
    await init();
    await searchUsers('Ada', usersResponse([user]));
    await harness.selectUser(/Ada Lovelace/);
    await harness.selectRole('USER');
    fixture.detectChanges();

    fixture.componentInstance.onSubmit();
    fixture.detectChanges();

    httpTestingController.expectOne(MEMBERS_URL).flush({ message: 'Already exists' }, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(await harness.getSubmitErrorText()).toContain('No members were added');
    const userErrors = await harness.getUserErrorTexts();
    expect(userErrors).toHaveLength(1);
    expect(userErrors[0]).toContain('Ada Lovelace');
    expect(userErrors[0]).toContain('This user is already a member of the application.');
  });

  it('should keep failed users selected when add members partially fails', async () => {
    const ada = fakeUser({ id: 'user-1', display_name: 'Ada Lovelace' });
    const bob = fakeUser({ id: 'user-2', display_name: 'Bob Martin' });
    await init();
    await searchUsers('Ada', usersResponse([ada]));
    await harness.selectUser(/Ada Lovelace/);
    await searchUsers('Bob', usersResponse([bob]));
    await harness.selectUser(/Bob Martin/);
    await harness.selectRole('USER');
    fixture.detectChanges();

    fixture.componentInstance.onSubmit();
    fixture.detectChanges();

    const requests = httpTestingController.match(req => req.url === MEMBERS_URL);
    expect(requests).toHaveLength(2);
    requests[0].flush({ id: 'member-1', role: 'USER' });
    requests[1].flush({ message: 'Server error' }, { status: 500, statusText: 'Error' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(await harness.getSubmitErrorText()).toContain('Some members were added');
    expect(await harness.getSelectedUserChipTexts()).toEqual([expect.stringContaining('Bob Martin')]);
    const userErrors = await harness.getUserErrorTexts();
    expect(userErrors).toHaveLength(1);
    expect(userErrors[0]).toContain('Bob Martin');
    expect(userErrors[0]).toContain('This member could not be added.');

    await harness.clickCancel();

    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('should show recoverable search error', async () => {
    await init();
    fixture.componentInstance.userControl.setValue('Ada');
    fixture.detectChanges();
    await new Promise(resolve => setTimeout(resolve, 350));
    fixture.detectChanges();

    httpTestingController
      .expectOne(req => req.url === USERS_SEARCH_URL)
      .flush({ message: 'Server error' }, { status: 500, statusText: 'Error' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await harness.getSearchErrorText()).toContain('An error occurred while searching users');
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
