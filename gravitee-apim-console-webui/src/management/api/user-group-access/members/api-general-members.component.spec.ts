/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { of } from 'rxjs';
import { MatOptionHarness } from '@angular/material/core/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ApiGeneralMembersComponent } from './api-general-members.component';
import { ApiGeneralMembersHarness } from './api-general-members.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { ApiUserGroupModule } from '../api-user-group.module';
import { RoleService } from '../../../../services-ngx/role.service';
import { Role } from '../../../../entities/role/role';
import { fakeRole } from '../../../../entities/role/role.fixture';
import { fakeSearchableUser } from '../../../../entities/user/searchableUser.fixture';
import { Api, fakeApiV1, fakeApiV4, fakeGroup, fakeGroupsResponse, MembersResponse } from '../../../../entities/management-api-v2';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';

describe('ApiGeneralMembersComponent', () => {
  let fixture: ComponentFixture<ApiGeneralMembersComponent>;
  let httpTestingController: HttpTestingController;
  let harness: ApiGeneralMembersHarness;

  const apiId = 'apiId';
  const roles: Role[] = [
    fakeRole({ name: 'PRIMARY_OWNER', default: false }),
    fakeRole({ name: 'OWNER', default: false }),
    fakeRole({ name: 'USER', default: true }),
  ];

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, MatIconTestingModule, ApiUserGroupModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId } } } },
        { provide: RoleService, useValue: { list: () => of(roles) } },
        { provide: GioTestingPermissionProvider, useValue: ['api-member-u', 'api-member-c', 'api-member-d'] },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This checks focus trap, set it to true to  avoid the warning
        isTabbable: () => true,
      },
    });

    fixture = TestBed.createComponent(ApiGeneralMembersComponent);
    httpTestingController = TestBed.inject(HttpTestingController);

    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiGeneralMembersHarness);

    fixture.detectChanges();
  });

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('API member change notification', () => {
    it('should uncheck box when notifications are off', async () => {
      const api = fakeApiV4({ id: apiId, disableMembershipNotifications: true });
      expectRequests(api);

      expect(await harness.isNotificationsToggleChecked()).toEqual(false);
    });

    it('should show save bar when toggle is toggles', async () => {
      const api = fakeApiV4({ id: apiId, disableMembershipNotifications: false });
      expectRequests(api);

      expect(await harness.isNotificationsToggleChecked()).toEqual(true);
      await harness.toggleNotificationToggle();

      expect(await harness.isSaveBarVisible()).toEqual(true);
    });

    it('should save notifications modifications when clicking on save bar', async () => {
      const api = fakeApiV4({ id: apiId, disableMembershipNotifications: false });
      expectRequests(api);

      await harness.toggleNotificationToggle();

      await harness.clickOnSave();

      // Setup all http calls for save + new OnInit execution
      // save
      expectApiGetRequest(api);
      const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}`, method: 'PUT' });
      expect(req.request.body.disableMembershipNotifications).toEqual(true);
      req.flush(api);
      // init
      expectApiGetRequest(api);
      expectApiMembersGetRequest();
      expectGetGroupsListRequest(api.groups);
      expectGetGroupMembersRequest();
    });
  });

  describe('List members', () => {
    it('should show all api members with roles', async () => {
      const api = fakeApiV4({ id: apiId });
      const members: MembersResponse = {
        data: [
          { id: '1', displayName: 'Mufasa', roles: [{ name: 'King', scope: 'API' }] },
          { id: '2', displayName: 'Simba', roles: [{ name: 'Prince', scope: 'API' }] },
        ],
      };
      expectRequests(api, members);

      expect((await harness.getTableRows()).length).toEqual(2);
      expect(await harness.getMembersName()).toEqual(['Mufasa', 'Simba']);
    });

    it('should make role readonly when user is PRIMARY OWNER', async () => {
      const api = fakeApiV4({ id: apiId, disableMembershipNotifications: false });
      const members: MembersResponse = { data: [{ id: '1', displayName: 'admin', roles: [{ name: 'PRIMARY_OWNER', scope: 'API' }] }] };
      expectRequests(api, members);

      expect(await harness.isMemberRoleSelectDisabled(0)).toEqual(true);
    });

    it('should make role editable when user is not PRIMARY OWNER', async () => {
      const api = fakeApiV4({ id: apiId, disableMembershipNotifications: false });
      const members: MembersResponse = { data: [{ id: '1', displayName: 'owner', roles: [{ name: 'OWNER', scope: 'API' }] }] };
      expectRequests(api, members);

      expect(await harness.isMemberRoleSelectDisabled(0)).toEqual(false);
      const roleOptions: MatOptionHarness[] = await harness.getMemberRoleSelectOptions(0);

      const options: string[] = await Promise.all(roleOptions.map(async opt => await opt.getText()));
      expect(options).toEqual(['PRIMARY_OWNER', 'OWNER', 'USER']);

      const poOption = await harness.getMemberRoleSelectOptions(0, { text: 'PRIMARY_OWNER' });
      expect(poOption.length).toEqual(1);
      expect(await poOption[0].isDisabled()).toEqual(true);
    });

    it('should call API to change role when clicking on save', async () => {
      const api = fakeApiV4({ id: apiId, disableMembershipNotifications: false });
      const members: MembersResponse = {
        data: [
          { id: '1', displayName: 'owner', roles: [{ name: 'PRIMARY_OWNER', scope: 'API' }] },
          {
            id: '2',
            displayName: 'other',
            roles: [{ name: 'OWNER', scope: 'API' }],
          },
        ],
      };
      expectRequests(api, members);

      await harness.getMemberRoleSelectForRowIndex(1).then(async select => {
        await select.open();
        return await select.clickOptions({ text: 'USER' });
      });

      await harness.clickOnSave();

      // Setup all http calls for save + new OnInit execution
      // save
      const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/members/2`, method: 'PUT' });
      req.flush({});
      expect(req.request.body).toEqual({ memberId: '2', roleName: 'USER' });
      // init
      expectRequests(api);
    });
  });

  describe('Delete a member', () => {
    it('should not allow to delete primary owner', async () => {
      const api = fakeApiV4({ id: apiId, disableMembershipNotifications: false });
      const members: MembersResponse = {
        data: [
          {
            id: '1',
            displayName: 'owner',
            roles: [{ name: 'PRIMARY_OWNER', scope: 'API' }],
          },
        ],
      };
      expectRequests(api, members);

      expect(await harness.isMemberDeleteButtonVisible(0)).toEqual(false);
    });

    it('should ask confirmation before delete member, and do nothing if canceled', async () => {
      const api = fakeApiV4({ id: apiId, disableMembershipNotifications: false });
      const members: MembersResponse = {
        data: [
          { id: '1', displayName: 'owner', roles: [{ name: 'PRIMARY_OWNER', scope: 'API' }] },
          {
            id: '2',
            displayName: 'user',
            roles: [{ name: 'USER', scope: 'API' }],
          },
        ],
      };
      expectRequests(api, members);

      expect(await harness.isMemberDeleteButtonVisible(1)).toEqual(true);

      await harness.getMemberDeleteButton(1).then(btn => btn.click());

      const confirmDialog = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
      expect(confirmDialog).toBeDefined();

      await confirmDialog.cancel();

      // no changes, no call to API
      expect((await harness.getTableRows()).length).toEqual(2);
    });

    it('should call the API if member deletion is confirmed', async () => {
      const api = fakeApiV4({ id: apiId, disableMembershipNotifications: false });
      const members: MembersResponse = {
        data: [
          { id: '1', displayName: 'owner', roles: [{ name: 'PRIMARY_OWNER', scope: 'API' }] },
          {
            id: '2',
            displayName: 'user',
            roles: [{ name: 'USER', scope: 'API' }],
          },
        ],
      };
      expectRequests(api, members);

      expect(await harness.isMemberDeleteButtonVisible(1)).toEqual(true);

      await harness.getMemberDeleteButton(1).then(btn => btn.click());

      const confirmDialog = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
      expect(confirmDialog).toBeDefined();

      await confirmDialog.confirm();
      expectDeleteMember(apiId, '2');

      expect((await harness.getTableRows()).length).toEqual(1);
      expect(await harness.getMembersName()).toEqual(['owner']);
    });
  });

  describe('Add a member', () => {
    it('should add add members with default role', async () => {
      const api = fakeApiV4({ id: apiId, disableMembershipNotifications: false });
      const members: MembersResponse = {
        data: [
          {
            id: '1',
            displayName: 'Existing User',
            roles: [{ name: 'PRIMARY_OWNER', scope: 'API' }],
          },
        ],
      };
      expectRequests(api, members);

      const memberToAdd = fakeSearchableUser({ id: 'user', displayName: 'User Id' });

      // Open dialog and add member
      await harness.addMember(memberToAdd, httpTestingController);

      // Expect member to be added
      expect(await harness.getMembersName()).toEqual(['Existing User', 'User Id']);
      // Expect default role to be selected
      const roleOptions: MatOptionHarness[] = await harness.getMemberRoleSelectOptions(1);
      const options = await Promise.all(
        roleOptions.map(async opt => ({
          text: await opt.getText(),
          isSelected: await opt.isSelected(),
        })),
      );
      expect(options).toEqual([
        {
          isSelected: false,
          text: 'PRIMARY_OWNER',
        },
        {
          isSelected: false,
          text: 'OWNER',
        },
        {
          isSelected: true,
          text: 'USER',
        },
      ]);

      // Save and expect POST request
      await harness.clickOnSave();
      const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/members`, method: 'POST' });
      expect(req.request.body).toEqual({ userId: memberToAdd.id, externalReference: memberToAdd.reference, roleName: 'USER' });
    });

    it('should add add members without id', async () => {
      const api = fakeApiV4({ id: apiId, disableMembershipNotifications: false });
      const members: MembersResponse = {
        data: [{ id: '1', displayName: 'Existing User', roles: [{ name: 'PRIMARY_OWNER', scope: 'API' }] }],
      };
      expectRequests(api, members);

      const memberToAdd = fakeSearchableUser({ id: undefined, displayName: 'User from LDAP' });

      // Open dialog and add member
      await harness.addMember(memberToAdd, httpTestingController);

      // Expect member to be added
      expect(await harness.getMembersName()).toEqual(['Existing User', 'User from LDAP']);

      // Save and expect POST request
      await harness.clickOnSave();
      const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/members`, method: 'POST' });
      expect(req.request.body).toEqual({ userId: undefined, externalReference: memberToAdd.reference, roleName: 'USER' });
    });
  });

  describe('V1 API', () => {
    it('should be in readonly mode', async () => {
      const api = fakeApiV1({ id: apiId });
      const members: MembersResponse = {
        data: [
          {
            id: '1',
            displayName: 'Existing User',
            roles: [{ name: 'PRIMARY_OWNER', scope: 'API' }],
          },
          {
            id: '2',
            displayName: 'User',
            roles: [{ name: 'USER', scope: 'API' }],
          },
        ],
      };
      expectRequests(api, members);

      // Cannot add member
      expect(await harness.canAddMember()).toBeFalsy();

      // Expect member to be added
      expect(await harness.getMembersName()).toEqual(['Existing User', 'User']);
      // Expect default role to be selected
      expect(await harness.canSelectMemberRole(1)).toBeFalsy();
    });
  });

  function expectRequests(api: Api, members: MembersResponse = { data: [] }) {
    expectApiGetRequest(api);
    expectGetGroupsListRequest(api.groups);
    expectApiMembersGetRequest(members);
    expectGetGroupMembersRequest();
  }

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  function expectApiMembersGetRequest(members: MembersResponse = { data: [] }) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/members?page=1&perPage=10`, method: 'GET' })
      .flush(members);
    fixture.detectChanges();
  }

  function expectDeleteMember(apiId: string, memberId: string) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/members/${memberId}`,
        method: 'DELETE',
      })
      .flush({});
    fixture.detectChanges();
  }

  function expectGetGroupMembersRequest() {
    // Call expected in child ApiPortalGroupMembers
    const groupId = fakeApiV4().groups[0];
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups/${groupId}/members?page=1&perPage=10`, method: 'GET' })
      .flush({ data: [], metadata: { groupName: 'group1' }, pagination: {} });
    fixture.detectChanges();
  }

  function expectGetGroupsListRequest(groups: string[]) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups?page=1&perPage=9999`, method: 'GET' })
      .flush(fakeGroupsResponse({ data: groups.map(id => fakeGroup({ id, name: id + '-name' })) }));
    fixture.detectChanges();
  }
});
