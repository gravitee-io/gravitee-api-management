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
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { ApplicationTabMembersComponent } from './application-tab-members.component';
import { ApplicationTabMembersComponentHarness } from './application-tab-members.component.harness';
import { ConfirmDialogComponent } from '../../../../components/confirm-dialog/confirm-dialog.component';
import { ConfirmDialogHarness } from '../../../../components/confirm-dialog/confirm-dialog.harness';
import { APPLICATION_PRIMARY_OWNER_ROLE_NAME, Application, ApplicationRole } from '../../../../entities/application/application';
import { MembersResponse } from '../../../../entities/member/member';
import { fakeMember, fakeMembersResponse } from '../../../../entities/member/member.fixtures';
import { fakeUserApplicationPermissions } from '../../../../entities/permission/permission.fixtures';
import { ApplicationService } from '../../../../services/application.service';
import { ConfigService } from '../../../../services/config.service';
import { CurrentUserService } from '../../../../services/current-user.service';
import { TESTING_BASE_URL } from '../../../../testing/app-testing.module';
import { ApplicationMemberEditDialogHarness } from '../application-member-edit-dialog/application-member-edit-dialog.component.harness';

const CURRENT_USER_ID = 'current-user-id';
const APPLICATION_ROLES: ApplicationRole[] = [
  { id: APPLICATION_PRIMARY_OWNER_ROLE_NAME, name: APPLICATION_PRIMARY_OWNER_ROLE_NAME, default: false, system: true },
  { id: 'OWNER', name: 'OWNER', default: false, system: false },
  { id: 'USER', name: 'USER', default: true, system: false },
];

describe('ApplicationTabMembersComponent', () => {
  let fixture: ComponentFixture<ApplicationTabMembersComponent>;
  let httpTestingController: HttpTestingController;
  let rootLoader: HarnessLoader;
  const applicationId = 'app-1';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationTabMembersComponent, ConfirmDialogComponent, MatDialogModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ApplicationService, useValue: { getApplicationRoles: () => of(APPLICATION_ROLES) } },
        { provide: ConfigService, useValue: { baseURL: TESTING_BASE_URL } },
        { provide: CurrentUserService, useValue: { user: () => ({ id: CURRENT_USER_ID }) } },
      ],
    })
      .overrideProvider(InteractivityChecker, { useValue: { isFocusable: () => true, isTabbable: () => true } })
      .compileComponents();

    fixture = TestBed.createComponent(ApplicationTabMembersComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    fixture.componentRef.setInput('applicationId', applicationId);
    fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R'] }));
  });

  afterEach(() => httpTestingController.verify());

  async function getHarness(): Promise<ApplicationTabMembersComponentHarness> {
    return TestbedHarnessEnvironment.harnessForFixture(fixture, ApplicationTabMembersComponentHarness);
  }

  async function flush(response: MembersResponse): Promise<ApplicationTabMembersComponentHarness> {
    fixture.detectChanges();
    const request = httpTestingController.expectOne(req => req.url.includes('members/_search'));
    expect(request.request.method).toBe('POST');
    request.flush(response);
    await fixture.whenStable();
    fixture.detectChanges();
    return getHarness();
  }

  async function openEditDialog(member = fakeMember()): Promise<ApplicationMemberEditDialogHarness> {
    const membersHarness = await flush(fakeMembersResponse([member]));
    const table = await membersHarness.getPaginatedTable();
    const editButton = await table!.getActionButton('edit');
    expect(editButton).not.toBeNull();
    await editButton!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return rootLoader.getHarness(ApplicationMemberEditDialogHarness);
  }

  it('should display members table', async () => {
    const harness = await flush(fakeMembersResponse([fakeMember()]));
    expect(await harness.getPaginatedTable()).not.toBeNull();
  });

  it('should show section title with total member count from pagination', async () => {
    await flush(fakeMembersResponse([fakeMember()], 42));
    const harness = await getHarness();
    const title = await harness.getSectionTitle();
    expect(title).not.toBeNull();
    expect((await title!.text())?.replace(/\s+/g, ' ').trim()).toBe('Members (42)');
  });

  it('should show Members (0) in section title when the list is empty', async () => {
    await flush(fakeMembersResponse([]));
    const harness = await getHarness();
    const title = await harness.getSectionTitle();
    expect(title).not.toBeNull();
    expect((await title!.text())?.replace(/\s+/g, ' ').trim()).toBe('Members (0)');
  });

  it('should show section title and error when search fails', async () => {
    fixture.detectChanges();
    const req = httpTestingController.expectOne(r => r.url.includes('members/_search'));
    req.flush({ error: 'Server error' }, { status: 500, statusText: 'Internal Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();
    const harness = await getHarness();
    const title = await harness.getSectionTitle();
    expect(title).not.toBeNull();
    expect((await title!.text())?.replace(/\s+/g, ' ').trim()).toBe('Members (0)');
    expect(await harness.getErrorMessage()).not.toBeNull();
  });

  it('should show loader until the first search response', async () => {
    // In-flight rxResource requests block fixture.whenStable(), which the harness API awaits;
    // fall back to a synchronous DOM check for the loading state (the rule's documented escape
    // hatch for rxResource streams that haven't completed yet).
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-loader')).not.toBeNull();
    const harness = await flush(fakeMembersResponse([fakeMember()]));
    expect(await harness.getLoader()).toBeNull();
  });

  it('should show an accessible error message when search fails', async () => {
    fixture.detectChanges();
    const request = httpTestingController.expectOne(req => req.url.includes('members/_search'));
    request.flush({ error: 'Server error' }, { status: 500, statusText: 'Internal Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();
    const harness = await getHarness();
    const alert = await harness.getErrorMessage();
    expect(alert).not.toBeNull();
    expect(await alert!.getAttribute('role')).toBe('alert');
  });

  it('should show empty state when no members are returned', async () => {
    const harness = await flush(fakeMembersResponse([]));
    expect(await harness.getEmptyState()).not.toBeNull();
  });

  it('should not render the paginated table when there are no members', async () => {
    const harness = await flush(fakeMembersResponse([], 0));
    expect(await harness.getPaginatedTable()).toBeNull();
  });

  it('should not show delete button when user lacks MEMBER[D] permission', async () => {
    const harness = await flush(
      fakeMembersResponse([fakeMember(), fakeMember({ id: 'member-2', role: APPLICATION_PRIMARY_OWNER_ROLE_NAME })]),
    );
    const table = await harness.getPaginatedTable();
    const actions = await table!.getActionButtons();
    expect(actions.length).toBe(0);
  });

  it('should hide the table and not call service when user lacks MEMBER[R] permission', async () => {
    fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: [] }));
    fixture.detectChanges();
    httpTestingController.expectNone(req => req.url.includes('members/_search'));
    const harness = await getHarness();
    expect(await harness.getPaginatedTable()).toBeNull();
  });

  it('should POST with page=2 when page changes', async () => {
    await flush(fakeMembersResponse([fakeMember()], 25));
    fixture.componentInstance.onPageChange(2);
    fixture.detectChanges();
    const request = httpTestingController.expectOne(req => req.url.includes('members/_search') && req.params.get('page') === '2');
    expect(request.request.method).toBe('POST');
    request.flush(fakeMembersResponse([fakeMember({ id: 'member-p2' })]));
    await fixture.whenStable();
  });

  describe('name cell', () => {
    it('should use display_name as primary label', async () => {
      const harness = await flush(fakeMembersResponse([fakeMember({ user: { id: 'u1', display_name: 'Alice Smith', _links: {} } })]));
      const userCell = await harness.getFirstUserCell();
      expect(await userCell.getPrimaryText()).toBe('Alice Smith');
    });

    it('should fall back to first_name + last_name when display_name absent', async () => {
      const harness = await flush(
        fakeMembersResponse([fakeMember({ user: { id: 'u1', first_name: 'Bob', last_name: 'Jones', _links: {} } })]),
      );
      const userCell = await harness.getFirstUserCell();
      expect(await userCell.getPrimaryText()).toBe('Bob Jones');
    });

    it('should fall back to user id when no name fields available', async () => {
      const harness = await flush(fakeMembersResponse([fakeMember({ user: { id: 'user-xyz', _links: {} } })]));
      const userCell = await harness.getFirstUserCell();
      expect(await userCell.getPrimaryText()).toBe('user-xyz');
    });

    it('should render email as caption text', async () => {
      const harness = await flush(
        fakeMembersResponse([fakeMember({ user: { id: 'u1', display_name: 'Alice', email: 'alice@example.com', _links: {} } })]),
      );
      const userCell = await harness.getFirstUserCell();
      expect(await userCell.getCaptionText()).toBe('alice@example.com');
    });

    it('should compute initials from display_name split by space', async () => {
      const harness = await flush(fakeMembersResponse([fakeMember({ user: { id: 'u1', display_name: 'Alice Smith', _links: {} } })]));
      const userCell = await harness.getFirstUserCell();
      expect(await userCell.getInitialsText()).toBe('AS');
    });

    it('should normalize multiple spaces when computing initials from display_name', async () => {
      const harness = await flush(fakeMembersResponse([fakeMember({ user: { id: 'u1', display_name: 'Alice    Smith', _links: {} } })]));
      const userCell = await harness.getFirstUserCell();
      expect(await userCell.getInitialsText()).toBe('AS');
    });

    it('should compute initials from first_name and last_name', async () => {
      const harness = await flush(
        fakeMembersResponse([fakeMember({ user: { id: 'u1', first_name: 'Bob', last_name: 'Jones', _links: {} } })]),
      );
      const userCell = await harness.getFirstUserCell();
      expect(await userCell.getInitialsText()).toBe('BJ');
    });

    it('should show (you) label for the current user', async () => {
      const harness = await flush(fakeMembersResponse([fakeMember({ user: { id: CURRENT_USER_ID, display_name: 'Me', _links: {} } })]));
      const userCell = await harness.getFirstUserCell();
      expect(await userCell.hasYouBadge()).toBe(true);
    });

    it('should not show (you) label for other users', async () => {
      const harness = await flush(fakeMembersResponse([fakeMember({ user: { id: 'other-user', display_name: 'Other', _links: {} } })]));
      const userCell = await harness.getFirstUserCell();
      expect(await userCell.hasYouBadge()).toBe(false);
    });
  });

  describe('search behavior', () => {
    it('should render search bar when canRead', () => {
      fixture.detectChanges();
      httpTestingController.expectOne(r => r.url.includes('members/_search')).flush(fakeMembersResponse([fakeMember()]));
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('app-search-bar')).toBeTruthy();
    });

    it('should not render search bar when canRead is false', () => {
      fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: [] }));
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('app-search-bar')).toBeNull();
    });

    it('should send displayName in filters on initial load', () => {
      fixture.detectChanges();
      const req = httpTestingController.expectOne(r => r.url.includes('members/_search'));
      expect(req.request.body).toEqual({ filters: { displayName: '' } });
      req.flush(fakeMembersResponse([fakeMember()]));
    });

    it('should send displayName filter when search term is set', () => {
      flush(fakeMembersResponse([fakeMember()]));
      fixture.componentInstance.onSearchTermChange('alice');
      fixture.detectChanges();
      const req = httpTestingController.expectOne(r => r.url.includes('members/_search') && r.params.get('page') === '1');
      expect(req.request.body).toEqual({ filters: { displayName: 'alice' } });
      req.flush(fakeMembersResponse([fakeMember()]));
    });

    it('should reset page to 1 when search term changes', () => {
      flush(fakeMembersResponse([fakeMember()], 25));
      fixture.componentInstance.onPageChange(2);
      fixture.detectChanges();
      httpTestingController.expectOne(r => r.params.get('page') === '2').flush(fakeMembersResponse([fakeMember()]));

      fixture.componentInstance.onSearchTermChange('alice');
      fixture.detectChanges();
      const req = httpTestingController.expectOne(r => r.url.includes('members/_search'));
      expect(req.request.params.get('page')).toBe('1');
      req.flush(fakeMembersResponse([fakeMember()]));
    });

    it('should show no-match empty state when search is active and returns 0 results', async () => {
      await flush(fakeMembersResponse([fakeMember()]));
      fixture.componentInstance.onSearchTermChange('zzz');
      fixture.detectChanges();
      const searchReq = httpTestingController.expectOne(r => r.url.includes('members/_search'));
      searchReq.flush(fakeMembersResponse([]));
      await fixture.whenStable();
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('[data-testid="members-search-no-match"]')).toBeTruthy();
      expect(fixture.nativeElement.querySelector('.members__empty-state p')?.textContent?.trim()).toBe('No members match your search.');
    });

    it('should show generic empty state when no search is active and returns 0 results', async () => {
      await flush(fakeMembersResponse([]));
      expect(fixture.nativeElement.querySelector('[data-testid="members-search-no-match"]')).toBeNull();
      expect(fixture.nativeElement.querySelector('.members__empty-state p')?.textContent?.trim()).toBe('No members found.');
    });

    it('should clear displayName in filters after clearing the search term', async () => {
      await flush(fakeMembersResponse([fakeMember()]));
      fixture.componentInstance.onSearchTermChange('alice');
      fixture.detectChanges();
      httpTestingController.expectOne(r => r.url.includes('members/_search')).flush(fakeMembersResponse([fakeMember()]));

      fixture.componentInstance.onSearchTermChange('');
      fixture.detectChanges();
      const req = httpTestingController.expectOne(r => r.url.includes('members/_search'));
      expect(req.request.body).toEqual({ filters: { displayName: '' } });
      req.flush(fakeMembersResponse([fakeMember()]));
    });
  });

  describe('role cell', () => {
    it('should mark primary owner via data-testid', async () => {
      const harness = await flush(fakeMembersResponse([fakeMember({ role: APPLICATION_PRIMARY_OWNER_ROLE_NAME })]));
      expect(await harness.isPrimaryOwnerRoleVisible()).toBe(true);
    });

    it('should not mark other roles as primary owner', async () => {
      const harness = await flush(fakeMembersResponse([fakeMember({ role: 'USER' })]));
      expect(await harness.isPrimaryOwnerRoleVisible()).toBe(false);
    });
  });

  describe('edit role action', () => {
    beforeEach(() => {
      fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R', 'U'] }));
    });

    it('should show edit button when user has MEMBER[U] permission', async () => {
      const harness = await flush(fakeMembersResponse([fakeMember()]));
      const table = await harness.getPaginatedTable();
      const editButton = await table!.getActionButton('edit');
      expect(editButton).not.toBeNull();
    });

    it('should not show edit button when user lacks MEMBER[U] permission', async () => {
      fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R'] }));
      const harness = await flush(fakeMembersResponse([fakeMember()]));
      const table = await harness.getPaginatedTable();
      const editButton = await table!.getActionButton('edit');
      expect(editButton).toBeNull();
    });

    it('should hide edit button for primary owner', async () => {
      const harness = await flush(fakeMembersResponse([fakeMember({ role: APPLICATION_PRIMARY_OWNER_ROLE_NAME })]));
      const table = await harness.getPaginatedTable();
      const editButton = await table!.getActionButton('edit');
      expect(editButton).toBeNull();
    });

    it('should open edit member dialog with current role preselected', async () => {
      const dialog = await openEditDialog(fakeMember({ role: 'USER' }));

      expect(await dialog.getRoleValueText()).toBe('USER');
    });

    it('should update member role and reload members table on success', async () => {
      const member = fakeMember({ id: undefined, role: 'USER', user: { id: 'user-1', display_name: 'Alice Smith', _links: {} } });
      const dialog = await openEditDialog(member);

      await dialog.selectRole('OWNER');
      await dialog.clickSave();
      fixture.detectChanges();

      const updateRequest = httpTestingController.expectOne(
        request =>
          request.url === `${TESTING_BASE_URL}/applications/${applicationId}/members/${member.user!.id}` && request.method === 'PUT',
      );
      expect(updateRequest.request.body).toEqual({ role: 'OWNER' });
      updateRequest.flush({ ...member, role: 'OWNER' });
      await fixture.whenStable();
      fixture.detectChanges();

      httpTestingController
        .expectOne(request => request.url.includes('members/_search'))
        .flush(fakeMembersResponse([{ ...member, role: 'OWNER' }]));
      await fixture.whenStable();
    });

    it('should not update member role when edit dialog is cancelled', async () => {
      const dialog = await openEditDialog(fakeMember({ role: 'USER' }));

      await dialog.selectRole('OWNER');
      await dialog.clickCancel();
      fixture.detectChanges();

      httpTestingController.expectNone(request => request.method === 'PUT');
      httpTestingController.expectNone(request => request.url.includes('members/_search'));
    });
  });

  describe('delete action', () => {
    beforeEach(() => {
      fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R', 'D'] }));
    });

    it('should show delete button when user has MEMBER[D] permission', async () => {
      const harness = await flush(fakeMembersResponse([fakeMember()]));
      const table = await harness.getPaginatedTable();
      const deleteButton = await table!.getActionButton('delete');
      expect(deleteButton).not.toBeNull();
    });

    it('should hide delete button for primary owner', async () => {
      const harness = await flush(fakeMembersResponse([fakeMember({ role: APPLICATION_PRIMARY_OWNER_ROLE_NAME })]));
      const table = await harness.getPaginatedTable();
      const deleteButton = await table!.getActionButton('delete');
      expect(deleteButton).toBeNull();
    });

    it('should disable delete button for current user', async () => {
      const harness = await flush(fakeMembersResponse([fakeMember({ user: { id: CURRENT_USER_ID, _links: {} } })]));
      const table = await harness.getPaginatedTable();
      const deleteButton = await table!.getActionButton('delete');
      expect(await deleteButton!.isDisabled()).toBe(true);
    });

    it('should not disable delete button for other members', async () => {
      const harness = await flush(fakeMembersResponse([fakeMember()]));
      const table = await harness.getPaginatedTable();
      const deleteButton = await table!.getActionButton('delete');
      expect(await deleteButton!.isDisabled()).toBe(false);
    });

    it('should open confirmation dialog on delete click', async () => {
      const harness = await flush(fakeMembersResponse([fakeMember()]));
      const table = await harness.getPaginatedTable();
      await table!.getActionButton('delete').then(btn => btn!.click());
      fixture.detectChanges();

      const dialog = await rootLoader.getHarnessOrNull(ConfirmDialogHarness);
      expect(dialog).not.toBeNull();
    });

    it('should send DELETE request and reload list on confirm', async () => {
      const member = fakeMember({ id: undefined, user: { id: 'user-1', display_name: 'Alice Smith', _links: {} } });
      const harness = await flush(fakeMembersResponse([member]));
      const table = await harness.getPaginatedTable();
      await table!.getActionButton('delete').then(btn => btn!.click());
      fixture.detectChanges();

      const dialog = await rootLoader.getHarnessOrNull(ConfirmDialogHarness);
      await dialog!.confirm();
      fixture.detectChanges();

      httpTestingController
        .expectOne(r => r.url === `${TESTING_BASE_URL}/applications/${applicationId}/members/${member.user!.id}` && r.method === 'DELETE')
        .flush(null, { status: 204, statusText: 'No Content' });
      fixture.detectChanges();

      // reload() triggers a new _search request; flush it before calling whenStable()
      httpTestingController.expectOne(r => r.url.includes('members/_search')).flush(fakeMembersResponse([member]));
      await fixture.whenStable();
    });

    it('should not send DELETE request when dialog is cancelled', async () => {
      const harness = await flush(fakeMembersResponse([fakeMember()]));
      const table = await harness.getPaginatedTable();
      await table!.getActionButton('delete').then(btn => btn!.click());
      fixture.detectChanges();

      const dialog = await rootLoader.getHarnessOrNull(ConfirmDialogHarness);
      await dialog!.cancel();
      fixture.detectChanges();

      httpTestingController.expectNone(r => r.method === 'DELETE');
    });
  });

  describe('add action', () => {
    it('should not show add members button when user lacks MEMBER[C] permission', async () => {
      const harness = await flush(fakeMembersResponse([fakeMember()]));

      expect(await harness.getAddMembersButton()).toBeNull();
    });

    it('should show add members button when user has MEMBER[C] permission', async () => {
      fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R', 'C'] }));
      const harness = await flush(fakeMembersResponse([fakeMember()]));

      expect(await harness.getAddMembersButton()).not.toBeNull();
    });

    it('should open add members dialog', async () => {
      fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R', 'C'] }));
      const harness = await flush(fakeMembersResponse([fakeMember()]));
      expect(await harness.getAddMembersButton()).not.toBeNull();

      await harness.clickAddMembersButton();
      await fixture.whenStable();
      fixture.detectChanges();

      expect(TestBed.inject(MatDialog).openDialogs).toHaveLength(1);
    });

    it('should reload members list after add members dialog closes with members added', async () => {
      fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R', 'C'] }));
      const harness = await flush(fakeMembersResponse([fakeMember()]));
      expect(await harness.getAddMembersButton()).not.toBeNull();
      await harness.clickAddMembersButton();
      await fixture.whenStable();
      fixture.detectChanges();

      TestBed.inject(MatDialog).openDialogs[0].close(true);
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();

      httpTestingController.expectOne(r => r.url.includes('members/_search')).flush(fakeMembersResponse([fakeMember({ id: 'member-2' })]));
      await fixture.whenStable();
    });
  });

  describe('transfer ownership action', () => {
    it('should not show transfer ownership button when user lacks MEMBER[U] permission', async () => {
      fixture.componentRef.setInput('application', fakeApplication(CURRENT_USER_ID));
      const harness = await flush(fakeMembersResponse([fakeMember()]));

      expect(await harness.getTransferOwnershipButton()).toBeNull();
    });

    it('should not show transfer ownership button when current user is not the application owner', async () => {
      fixture.componentRef.setInput('application', fakeApplication('other-owner-id'));
      fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R', 'U'] }));
      const harness = await flush(fakeMembersResponse([fakeMember()]));

      expect(await harness.getTransferOwnershipButton()).toBeNull();
    });

    it('should show transfer ownership button for application owner with MEMBER[U] permission', async () => {
      fixture.componentRef.setInput('application', fakeApplication(CURRENT_USER_ID));
      fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R', 'U'] }));
      const harness = await flush(fakeMembersResponse([fakeMember()]));

      expect(await harness.getTransferOwnershipButton()).not.toBeNull();
    });

    it('should open transfer ownership dialog', async () => {
      fixture.componentRef.setInput('application', fakeApplication(CURRENT_USER_ID));
      fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R', 'U'] }));
      const harness = await flush(
        fakeMembersResponse([
          fakeMember({ id: CURRENT_USER_ID, role: APPLICATION_PRIMARY_OWNER_ROLE_NAME, user: { id: CURRENT_USER_ID, _links: {} } }),
          fakeMember({ id: 'member-2', user: { id: 'user-2', display_name: 'Bob Martin', _links: {} } }),
        ]),
      );

      await harness.clickTransferOwnershipButton();
      await fixture.whenStable();
      fixture.detectChanges();

      expect(TestBed.inject(MatDialog).openDialogs).toHaveLength(1);
    });

    it('should reload members list after transfer ownership dialog closes with success', async () => {
      fixture.componentRef.setInput('application', fakeApplication(CURRENT_USER_ID));
      fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R', 'U'] }));
      const harness = await flush(fakeMembersResponse([fakeMember()]));

      await harness.clickTransferOwnershipButton();
      await fixture.whenStable();
      fixture.detectChanges();

      TestBed.inject(MatDialog).openDialogs[0].close(true);
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();

      httpTestingController
        .expectOne(r => r.url === `${TESTING_BASE_URL}/permissions` && r.params.get('applicationId') === applicationId)
        .flush(fakeUserApplicationPermissions({ MEMBER: ['R'] }));
      await fixture.whenStable();
      fixture.detectChanges();

      httpTestingController.expectOne(r => r.url.includes('members/_search')).flush(fakeMembersResponse([fakeMember({ id: 'member-2' })]));
      await fixture.whenStable();

      expect(await harness.getTransferOwnershipButton()).toBeNull();
    });
  });
});

function fakeApplication(ownerId: string): Application {
  return {
    id: 'app-1',
    name: 'Application',
    owner: {
      id: ownerId,
      first_name: 'Current',
      last_name: 'Owner',
      display_name: 'Current Owner',
      email: 'owner@example.com',
      editable_profile: false,
      customFields: {
        city: '',
        job_position: '',
      },
      _links: {
        avatar: '',
        notifications: '',
        self: '',
      },
    },
    settings: {},
  };
}
