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

import { ApiPortalMembersComponent } from './api-portal-members.component';
import { ApiPortalMembersHarness } from './api-portal-members.harness';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { ApiPortalUserGroupModule } from '../api-portal-user-group.module';
import { fakeApi } from '../../../../../entities/api/Api.fixture';
import { Api, ApiMember } from '../../../../../entities/api';
import { UsersService } from '../../../../../services-ngx/users.service';
import { UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { RoleService } from '../../../../../services-ngx/role.service';
import { Role } from '../../../../../entities/role/role';
import { fakeRole } from '../../../../../entities/role/role.fixture';

describe('ApiPortalMembersComponent', () => {
  let fixture: ComponentFixture<ApiPortalMembersComponent>;
  let httpTestingController: HttpTestingController;
  let harness: ApiPortalMembersHarness;
  const apiId = 'apiId';
  const roles: Role[] = [fakeRole({ name: 'PRIMARY_OWNER' }), fakeRole({ name: 'OWNER' }), fakeRole({ name: 'USER' })];

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, MatIconTestingModule, ApiPortalUserGroupModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId } },
        { provide: UsersService, useValue: { getUserAvatar: () => 'avatar' } },
        { provide: RoleService, useValue: { list: () => of(roles) } },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This checks focus trap, set it to true to  avoid the warning
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

      expect(await harness.isNotificationsCheckboxChecked()).toEqual(false);
    });

    it('should show save bar when checkbox is toggles', async () => {
      const api = fakeApi({ id: apiId, disable_membership_notifications: false });
      expectApiGetRequest(api);
      expectApiMembersGetRequest();

      expect(await harness.isNotificationsCheckboxChecked()).toEqual(true);
      await harness.toggleNotificationCheckbox();

      expect(await harness.isSaveBarVisible()).toEqual(true);
    });

    it('should save notifications modifications when clicking on save bar', async () => {
      const api = fakeApi({ id: apiId, disable_membership_notifications: false });
      expectApiGetRequest(api);
      expectApiMembersGetRequest();

      await harness.toggleNotificationCheckbox();

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

      expect((await harness.getTableRows()).length).toEqual(2);
      expect(await harness.getMembersName()).toEqual(['Mufasa', 'Simba']);
    });

    it('should show username when no display name', async () => {
      const api = fakeApi({ id: apiId });
      const members: ApiMember[] = [{ id: '1', username: 'bot', role: 'USER' }];
      expectApiGetRequest(api);
      expectApiMembersGetRequest(members);

      expect(await harness.getMembersName()).toEqual(['bot']);
    });

    it('should show username next to displayName when no display name', async () => {
      const api = fakeApi({ id: apiId });
      const members: ApiMember[] = [{ id: '1', displayName: 'Regular user', username: 'bot', role: 'USER' }];
      expectApiGetRequest(api);
      expectApiMembersGetRequest(members);

      expect(await harness.getMembersName()).toEqual(['Regular user (bot)']);
    });

    it('should make role readonly when user is PRIMARY OWNER', async () => {
      const api = fakeApi({ id: apiId, disable_membership_notifications: false });
      const members: ApiMember[] = [{ id: '1', displayName: 'admin', role: 'PRIMARY_OWNER' }];
      expectApiGetRequest(api);
      expectApiMembersGetRequest(members);

      expect(await harness.isMemberRoleSelectDisabled(0)).toEqual(true);
    });

    it('should make role editable when user is not PRIMARY OWNER', async () => {
      const api = fakeApi({ id: apiId, disable_membership_notifications: false });
      const members: ApiMember[] = [{ id: '1', displayName: 'owner', role: 'OWNER' }];
      expectApiGetRequest(api);
      expectApiMembersGetRequest(members);

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
    });
  });

  describe('Delete a member', () => {
    it('should not allow to delete primary owner', async () => {
      const api = fakeApi({ id: apiId, disable_membership_notifications: false });
      const members: ApiMember[] = [{ id: '1', displayName: 'owner', role: 'PRIMARY_OWNER' }];
      expectApiGetRequest(api);
      expectApiMembersGetRequest(members);

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
});
