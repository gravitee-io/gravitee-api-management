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

import { ApiPortalMembersComponent } from './api-portal-members.component';
import { ApiPortalMembersHarness } from './api-portal-members.harness';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { ApiPortalUserGroupModule } from '../api-portal-user-group.module';
import { fakeApi } from '../../../../../entities/api/Api.fixture';
import { Api, ApiMember } from '../../../../../entities/api';
import { CurrentUserService, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { RoleService } from '../../../../../services-ngx/role.service';
import { Role } from '../../../../../entities/role/role';
import { fakeRole } from '../../../../../entities/role/role.fixture';
import { User } from '../../../../../entities/user';
import { fakeSearchableUser } from '../../../../../entities/user/searchableUser.fixture';

describe('ApiPortalMembersComponent', () => {
  let fixture: ComponentFixture<ApiPortalMembersComponent>;
  let httpTestingController: HttpTestingController;
  let harness: ApiPortalMembersHarness;

  const currentUser = new User();
  currentUser.userPermissions = ['api-member-u', 'api-member-c', 'api-member-d'];
  const apiId = 'apiId';
  const roles: Role[] = [
    fakeRole({ name: 'PRIMARY_OWNER', default: false }),
    fakeRole({ name: 'OWNER', default: false }),
    fakeRole({ name: 'USER', default: true }),
  ];

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, MatIconTestingModule, ApiPortalUserGroupModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId } },
        { provide: RoleService, useValue: { list: () => of(roles) } },
        { provide: CurrentUserService, useValue: { currentUser } },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This checks focus trap, set it to true to  avoid the warning
        isTabbable: () => true,
      },
    });

    fixture = TestBed.createComponent(ApiPortalMembersComponent);
    httpTestingController = TestBed.inject(HttpTestingController);

    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiPortalMembersHarness);

    fixture.detectChanges();
  });

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('API member change notification', () => {
    it('should uncheck box when notifications are off', async () => {
      const api = fakeApi({ id: apiId, disable_membership_notifications: true });
      expectApiGetRequest(api);
      expectApiMembersGetRequest();
      expectGetGroupMembersRequest();

      expect(await harness.isNotificationsToggleChecked()).toEqual(false);
    });

    it('should show save bar when toggle is toggles', async () => {
      const api = fakeApi({ id: apiId, disable_membership_notifications: false });
      expectApiGetRequest(api);
      expectApiMembersGetRequest();
      expectGetGroupMembersRequest();

      expect(await harness.isNotificationsToggleChecked()).toEqual(true);
      await harness.toggleNotificationToggle();

      expect(await harness.isSaveBarVisible()).toEqual(true);
    });

    it('should save notifications modifications when clicking on save bar', async () => {
      const api = fakeApi({ id: apiId, disable_membership_notifications: false });
      expectApiGetRequest(api);
      expectApiMembersGetRequest();
      expectGetGroupMembersRequest();

      await harness.toggleNotificationToggle();

      await harness.clickOnSave();

      // Setup all http calls for save + new OnInit execution
      // save
      expectApiGetRequest(api);
      const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}`, method: 'PUT' });
      expect(req.request.body.disable_membership_notifications).toEqual(true);
      req.flush(api);
      // init
      expectApiGetRequest(api);
      expectApiMembersGetRequest();
      expectGetGroupMembersRequest();
    });
  });

  describe('List members', () => {
    it('should show all api members with roles', async () => {
      const api = fakeApi({ id: apiId });
      const members: ApiMember[] = [
        { id: '1', displayName: 'Mufasa', role: 'King' },
        { id: '2', displayName: 'Simba', role: 'Prince' },
      ];
      expectApiGetRequest(api);
      expectApiMembersGetRequest(members);
      expectGetGroupMembersRequest();

      expect((await harness.getTableRows()).length).toEqual(2);
      expect(await harness.getMembersName()).toEqual(['Mufasa', 'Simba']);
    });

    it('should make role readonly when user is PRIMARY OWNER', async () => {
      const api = fakeApi({ id: apiId, disable_membership_notifications: false });
      const members: ApiMember[] = [{ id: '1', displayName: 'admin', role: 'PRIMARY_OWNER' }];
      expectApiGetRequest(api);
      expectApiMembersGetRequest(members);
      expectGetGroupMembersRequest();

      expect(await harness.isMemberRoleSelectDisabled(0)).toEqual(true);
    });

    it('should make role editable when user is not PRIMARY OWNER', async () => {
      const api = fakeApi({ id: apiId, disable_membership_notifications: false });
      const members: ApiMember[] = [{ id: '1', displayName: 'owner', role: 'OWNER' }];
      expectApiGetRequest(api);
      expectApiMembersGetRequest(members);
      expectGetGroupMembersRequest();

      expect(await harness.isMemberRoleSelectDisabled(0)).toEqual(false);
      const roleOptions: MatOptionHarness[] = await harness.getMemberRoleSelectOptions(0);

      const options: string[] = await Promise.all(roleOptions.map(async (opt) => await opt.getText()));
      expect(options).toEqual(['PRIMARY_OWNER', 'OWNER', 'USER']);

      const poOption = await harness.getMemberRoleSelectOptions(0, { text: 'PRIMARY_OWNER' });
      expect(poOption.length).toEqual(1);
      expect(await poOption[0].isDisabled()).toEqual(true);
    });

    it('should call API to change role when clicking on save', async () => {
      const api = fakeApi({ id: apiId, disable_membership_notifications: false });
      const members: ApiMember[] = [
        { id: '1', displayName: 'owner', role: 'PRIMARY_OWNER' },
        { id: '2', displayName: 'other', role: 'OWNER' },
      ];
      expectApiGetRequest(api);
      expectApiMembersGetRequest(members);
      expectGetGroupMembersRequest();

      await harness.getMemberRoleSelectForRowIndex(1).then(async (select) => {
        await select.open();
        return await select.clickOptions({ text: 'USER' });
      });

      await harness.clickOnSave();

      // Setup all http calls for save + new OnInit execution
      // save
      const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/members`, method: 'POST' });
      req.flush({});
      expect(req.request.body).toEqual({ id: '2', reference: undefined, role: 'USER' });
      // init
      expectApiGetRequest(api);
      expectApiMembersGetRequest();
      expectGetGroupMembersRequest();
    });
  });

  describe('Delete a member', () => {
    it('should not allow to delete primary owner', async () => {
      const api = fakeApi({ id: apiId, disable_membership_notifications: false });
      const members: ApiMember[] = [{ id: '1', displayName: 'owner', role: 'PRIMARY_OWNER' }];
      expectApiGetRequest(api);
      expectApiMembersGetRequest(members);
      expectGetGroupMembersRequest();

      expect(await harness.isMemberDeleteButtonVisible(0)).toEqual(false);
    });

    it('should ask confirmation before delete member, and do nothing if canceled', async () => {
      const api = fakeApi({ id: apiId, disable_membership_notifications: false });
      const members: ApiMember[] = [
        { id: '1', displayName: 'owner', role: 'PRIMARY_OWNER' },
        { id: '2', displayName: 'user', role: 'USER' },
      ];
      expectApiGetRequest(api);
      expectApiMembersGetRequest(members);
      expectGetGroupMembersRequest();

      expect(await harness.isMemberDeleteButtonVisible(1)).toEqual(true);

      await harness.getMemberDeleteButton(1).then((btn) => btn.click());

      const confirmDialog = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
      expect(confirmDialog).toBeDefined();

      await confirmDialog.cancel();

      // no changes, no call to API
      expect((await harness.getTableRows()).length).toEqual(2);
    });

    it('should call the API if member deletion is confirmed', async () => {
      const api = fakeApi({ id: apiId, disable_membership_notifications: false });
      const members: ApiMember[] = [
        { id: '1', displayName: 'owner', role: 'PRIMARY_OWNER' },
        { id: '2', displayName: 'user', role: 'USER' },
      ];
      expectApiGetRequest(api);
      expectApiMembersGetRequest(members);
      expectGetGroupMembersRequest();

      expect(await harness.isMemberDeleteButtonVisible(1)).toEqual(true);

      await harness.getMemberDeleteButton(1).then((btn) => btn.click());

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
      const api = fakeApi({ id: apiId, disable_membership_notifications: false });
      const members: ApiMember[] = [{ id: '1', displayName: 'Existing User', role: 'PRIMARY_OWNER' }];
      expectApiGetRequest(api);
      expectApiMembersGetRequest(members);
      expectGetGroupMembersRequest();

      const memberToAdd = fakeSearchableUser({ id: 'user', displayName: 'User Id' });

      // Open dialog and add member
      await harness.addMember(memberToAdd, httpTestingController);

      // Expect member to be added
      expect(await harness.getMembersName()).toEqual(['Existing User', 'User Id']);
      // Expect default role to be selected
      const roleOptions: MatOptionHarness[] = await harness.getMemberRoleSelectOptions(1);
      const options = await Promise.all(
        roleOptions.map(async (opt) => ({
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
      const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/members`, method: 'POST' });
      expect(req.request.body).toEqual({ id: memberToAdd.id, reference: memberToAdd.reference, role: 'USER' });
    });

    it('should add add members without id', async () => {
      const api = fakeApi({ id: apiId, disable_membership_notifications: false });
      const members: ApiMember[] = [{ id: '1', displayName: 'Existing User', role: 'PRIMARY_OWNER' }];
      expectApiGetRequest(api);
      expectApiMembersGetRequest(members);
      expectGetGroupMembersRequest();

      const memberToAdd = fakeSearchableUser({ id: undefined, displayName: 'User from LDAP' });

      // Open dialog and add member
      await harness.addMember(memberToAdd, httpTestingController);

      // Expect member to be added
      expect(await harness.getMembersName()).toEqual(['Existing User', 'User from LDAP']);

      // Save and expect POST request
      await harness.clickOnSave();
      const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/members`, method: 'POST' });
      expect(req.request.body).toEqual({ id: undefined, reference: memberToAdd.reference, role: 'USER' });
    });
  });

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  function expectApiMembersGetRequest(members: ApiMember[] = []) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/members`, method: 'GET' }).flush(members);
    fixture.detectChanges();
  }

  function expectDeleteMember(apiId: string, memberId: string) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/members?user=${memberId}`,
        method: 'DELETE',
      })
      .flush({});
    fixture.detectChanges();
  }

  function expectGetGroupMembersRequest() {
    // Call expected in child ApiPortalGroupMembers
    const groupId = fakeApi().groups[0];
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups/${groupId}/members?page=1&perPage=10`, method: 'GET' })
      .flush({ data: [], metadata: { groupName: 'group1' }, pagination: {} });
    fixture.detectChanges();
  }
});
