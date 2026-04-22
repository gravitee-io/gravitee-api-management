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
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';

import { ApplicationTabMembersComponent } from './application-tab-members.component';
import { ApplicationTabMembersComponentHarness } from './application-tab-members.component.harness';
import { MembersResponse } from '../../../../entities/member/member';
import { fakeMember, fakeMembersResponse } from '../../../../entities/member/member.fixtures';
import { fakeUserApplicationPermissions } from '../../../../entities/permission/permission.fixtures';
import { ConfigService } from '../../../../services/config.service';
import { CurrentUserService } from '../../../../services/current-user.service';
import { TESTING_BASE_URL } from '../../../../testing/app-testing.module';

const CURRENT_USER_ID = 'current-user-id';

describe('ApplicationTabMembersComponent', () => {
  let fixture: ComponentFixture<ApplicationTabMembersComponent>;
  let httpTestingController: HttpTestingController;
  const applicationId = 'app-1';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationTabMembersComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ConfigService, useValue: { baseURL: TESTING_BASE_URL } },
        { provide: CurrentUserService, useValue: { user: () => ({ id: CURRENT_USER_ID }) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApplicationTabMembersComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
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

  it('should display members table', async () => {
    const harness = await flush(fakeMembersResponse([fakeMember()]));
    expect(await harness.getPaginatedTable()).not.toBeNull();
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

  it('should render no action buttons in phase 01', async () => {
    const harness = await flush(fakeMembersResponse([fakeMember(), fakeMember({ id: 'member-2', role: 'PRIMARY_OWNER' })]));
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

    it('should render email as secondary text', async () => {
      const harness = await flush(
        fakeMembersResponse([fakeMember({ user: { id: 'u1', display_name: 'Alice', email: 'alice@example.com', _links: {} } })]),
      );
      const userCell = await harness.getFirstUserCell();
      expect(await userCell.getSecondaryText()).toBe('alice@example.com');
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

  describe('role cell', () => {
    it('should mark PRIMARY_OWNER via data-testid', async () => {
      const harness = await flush(fakeMembersResponse([fakeMember({ role: 'PRIMARY_OWNER' })]));
      expect(await harness.isPrimaryOwnerRoleVisible()).toBe(true);
    });

    it('should not mark other roles as PRIMARY_OWNER', async () => {
      const harness = await flush(fakeMembersResponse([fakeMember({ role: 'USER' })]));
      expect(await harness.isPrimaryOwnerRoleVisible()).toBe(false);
    });
  });
});
