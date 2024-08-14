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
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { set } from 'lodash';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatButtonToggleGroupHarness } from '@angular/material/button-toggle/testing';
import { ActivatedRoute } from '@angular/router';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { IntegrationUserGroupModule } from '../integration-user-group.module';
import { Role } from '../../../../entities/role/role';
import { fakeRole } from '../../../../entities/role/role.fixture';
import { GioFormUserAutocompleteHarness } from '../../../../shared/components/gio-user-autocomplete/gio-form-user-autocomplete.harness';
import { fakeSearchableUser } from '../../../../entities/user/searchableUser.fixture';
import { SearchableUser } from '../../../../entities/user/searchableUser';
import { IntegrationGeneralMembersComponent } from '../members/integration-general-members.component';
import { Api, fakeApiV2, fakeApiV4, fakeGroup, fakeGroupsResponse, Group, MembersResponse } from '../../../../entities/management-api-v2';
import { IntegrationGeneralMembersHarness } from '../members/integration-general-members.harness';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { fakeMember } from '../../../../entities/management-api-v2/member/member.fixture';
import { Constants } from '../../../../entities/Constants';

xdescribe('IntegrationGeneralTransferOwnershipComponent', () => {
  const apiId = 'apiId';
  const defaultRole = fakeRole({ name: 'DEFAULT_ROLE', default: true });
  const poRole = fakeRole({ name: 'PRIMARY_OWNER', default: false });
  const role1 = fakeRole({ name: 'ROLE_1', default: false });
  const defaultRoles: Role[] = [poRole, role1, defaultRole];

  let fixture: ComponentFixture<IntegrationGeneralMembersComponent>;
  let harness: IntegrationGeneralMembersHarness;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  describe('Hybrid mode', () => {
    beforeEach(async () => {
      TestBed.configureTestingModule({
        imports: [NoopAnimationsModule, GioTestingModule, MatIconTestingModule, IntegrationUserGroupModule],
        providers: [
          { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId } } } },
          {
            provide: Constants,
            useFactory: () => {
              const constants = CONSTANTS_TESTING;
              set(constants, 'env.settings.api.primaryOwnerMode', 'HYBRID');
              return constants;
            },
          },
          { provide: GioTestingPermissionProvider, useValue: ['api-definition-u', 'api-member-u'] },
        ],
      }).overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true,
          isTabbable: () => true,
        },
      });

      fixture = TestBed.createComponent(IntegrationGeneralMembersComponent);
      harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, IntegrationGeneralMembersHarness);
      rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
      httpTestingController = TestBed.inject(HttpTestingController);
      fixture.detectChanges();
    });

    afterEach(() => {
      jest.clearAllMocks();
      httpTestingController.verify();
    });

    it('should transfer ownership to user', async () => {
      const api = fakeApiV2({ id: apiId, groups: [] });
      await expectGetRequests(api, [], { data: [fakeMember({ id: '1', displayName: 'Joe', roles: [defaultRole] })] });

      expect(await harness.isTransferOwnershipVisible()).toBeTruthy();
      await harness.transferOwnershipClick();

      // Select User mode
      const userOrGroupRadio = await rootLoader.getHarness(
        MatButtonToggleGroupHarness.with({ selector: '[formControlName="userOrGroup"]' }),
      );
      const otherUserButton = await userOrGroupRadio.getToggles({ text: 'Other user' });
      await otherUserButton[0].check();

      // Search and select user
      const userSelect = await rootLoader.getHarness(GioFormUserAutocompleteHarness);
      await userSelect.setSearchText('Joe');
      respondToUserSearchRequest('Joe', [fakeSearchableUser({ displayName: 'Joe' })]);
      await userSelect.selectOption({ text: 'Joe' });
      respondToUserSearchRequest('Joe', [fakeSearchableUser({ displayName: 'Joe' })]);

      // Select role
      const roleSelect = await rootLoader.getHarness(MatSelectHarness.with({ selector: '[formControlName="roleId"]' }));
      await roleSelect.open();
      const roleOptions = await roleSelect.getOptions();
      expect(roleOptions.length).toBe(2);
      // Check that the default role is selected
      expect(await roleSelect.getValueText()).toBe('DEFAULT_ROLE');
      await roleSelect.clickOptions({ text: 'ROLE_1' });

      // Submit
      const transferBtn = await rootLoader.getHarness(MatButtonHarness.with({ text: 'Transfer' }));
      expect(await transferBtn.isDisabled()).toBeFalsy();
      await transferBtn.click();
      fixture.detectChanges();

      // Check request
      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/_transfer-ownership`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({
        userId: '1d4fae8c-3705-43ab-8fae-8c370543abf3',
        userReference: expect.any(String),
        poRole: 'ROLE_1',
        userType: 'USER',
      });
    });

    it('should transfer ownership to api members', async () => {
      const api = fakeApiV4({ id: apiId, groups: [] });
      await expectGetRequests(api, [], { data: [fakeMember({ id: '1', displayName: 'Joe', roles: [defaultRole] })] });

      expect(await harness.isTransferOwnershipVisible()).toBeTruthy();
      await harness.transferOwnershipClick();

      // Select User mode
      const userOrGroupRadio = await rootLoader.getHarness(
        MatButtonToggleGroupHarness.with({ selector: '[formControlName="userOrGroup"]' }),
      );
      const otherUserButton = await userOrGroupRadio.getToggles({ text: 'API member' });
      await otherUserButton[0].check();

      // Search and select user
      const userSelect = await rootLoader.getHarness(MatSelectHarness.with({ selector: '[formControlName="user"]' }));
      await userSelect.clickOptions({ text: 'Joe' });

      // Select role
      const roleSelect = await rootLoader.getHarness(MatSelectHarness.with({ selector: '[formControlName="roleId"]' }));
      await roleSelect.open();
      const roleOptions = await roleSelect.getOptions();
      expect(roleOptions.length).toBe(2);
      // Check that the default role is selected
      expect(await roleSelect.getValueText()).toBe('DEFAULT_ROLE');
      await roleSelect.clickOptions({ text: 'ROLE_1' });

      // Submit
      const transferBtn = await rootLoader.getHarness(MatButtonHarness.with({ text: 'Transfer' }));
      expect(await transferBtn.isDisabled()).toBeFalsy();
      await transferBtn.click();
      fixture.detectChanges();

      // Check request
      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/_transfer-ownership`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({
        userId: '1',
        userReference: undefined,
        poRole: 'ROLE_1',
        userType: 'USER',
      });
    });

    it('should transfer ownership to group', async () => {
      const api = fakeApiV4({ id: apiId, groups: [] });
      await expectGetRequests(
        api,
        [
          fakeGroup({ id: 'group1', name: 'Group 1', apiPrimaryOwner: 'apo-uuid' }),
          fakeGroup({ id: 'group2', name: 'Group null', apiPrimaryOwner: null }),
        ],
        { data: [fakeMember({ id: '1', displayName: 'Joe', roles: [defaultRole] })] },
      );

      expect(await harness.isTransferOwnershipVisible()).toBeTruthy();
      await harness.transferOwnershipClick();

      // Select Group mode
      const userOrGroupRadio = await rootLoader.getHarness(
        MatButtonToggleGroupHarness.with({ selector: '[formControlName="userOrGroup"]' }),
      );
      const otherUserButton = await userOrGroupRadio.getToggles({ text: 'Primary owner group' });
      await otherUserButton[0].check();

      // Select group
      const groupSelect = await rootLoader.getHarness(MatSelectHarness.with({ selector: '[formControlName="groupId"]' }));
      await groupSelect.open();
      const options = await groupSelect.getOptions();
      expect(options.length).toBe(1);
      await groupSelect.clickOptions({ text: 'Group 1' });

      // Not select role -> use default role

      // Submit
      const transferBtn = await rootLoader.getHarness(MatButtonHarness.with({ text: 'Transfer' }));
      expect(await transferBtn.isDisabled()).toBeFalsy();
      await transferBtn.click();
      fixture.detectChanges();

      // Check request
      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/_transfer-ownership`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({
        userId: 'group1',
        userReference: null,
        poRole: 'DEFAULT_ROLE',
        userType: 'GROUP',
      });
    });
  });

  describe('Group mode', () => {
    beforeEach(async () => {
      TestBed.configureTestingModule({
        imports: [NoopAnimationsModule, GioTestingModule, MatIconTestingModule, IntegrationUserGroupModule],
        providers: [
          { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId } } } },
          {
            provide: Constants,
            useFactory: () => {
              const constants = CONSTANTS_TESTING;
              set(constants, 'env.settings.api.primaryOwnerMode', 'GROUP');
              return constants;
            },
          },
          { provide: GioTestingPermissionProvider, useValue: ['api-definition-u', 'api-member-u'] },
        ],
      });

      fixture = TestBed.createComponent(IntegrationGeneralMembersComponent);
      harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, IntegrationGeneralMembersHarness);
      rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
      httpTestingController = TestBed.inject(HttpTestingController);
      fixture.detectChanges();
    });

    afterEach(() => {
      jest.clearAllMocks();
      httpTestingController.verify();
    });

    it('should only allow transfer ownership to group', async () => {
      const api = fakeApiV4({ id: apiId, groups: [] });
      await expectGetRequests(
        api,
        [
          fakeGroup({ id: 'group1', name: 'group1', apiPrimaryOwner: 'apo-uuid' }),
          fakeGroup({ id: 'group2', name: 'group2', apiPrimaryOwner: 'apo-uuid' }),
        ],
        { data: [fakeMember({ id: '1', displayName: 'Joe', roles: [defaultRole] })] },
      );

      expect(await harness.isTransferOwnershipVisible()).toBeTruthy();
      await harness.transferOwnershipClick();

      // No toggle from selection mode
      expect(
        (await rootLoader.getAllHarnesses(MatButtonToggleGroupHarness.with({ selector: '[formControlName=groupId]' }))).length,
      ).toEqual(0);
      // Only one select with groups
      const groupOptions = await rootLoader
        .getHarness(MatSelectHarness.with({ selector: '[formControlName="groupId"]' }))
        .then(async (harness) => {
          await harness.open();
          return await harness.getOptions();
        });
      expect(await Promise.all(groupOptions.map(async (opt) => await opt.getText()))).toEqual(['group1', 'group2']);
    });
  });

  async function expectGetRequests(api: Api, groups: Group[] = [], members: MembersResponse = { data: [] }) {
    expectApiGetRequest(api);
    expectGetGroupsListRequest(groups);
    expectApiMembersGetRequest(api, members);
    expectApiRoleGetRequest(defaultRoles);
  }

  function expectGetGroupsListRequest(groups: Group[]) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups?page=1&perPage=9999`, method: 'GET' })
      .flush(fakeGroupsResponse({ data: groups }));
    fixture.detectChanges();
  }

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  function expectApiRoleGetRequest(roles: Role[] = []) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/API/roles`, method: 'GET' })
      .flush(roles);
  }

  function respondToUserSearchRequest(searchTerm: string, searchableUsers: SearchableUser[]) {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.org.baseURL}/search/users?q=${searchTerm}`,
      })
      .flush(searchableUsers);
  }

  function expectApiMembersGetRequest(api: Api, members: MembersResponse = { data: [] }) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}/members`, method: 'GET' }).flush(members);
    fixture.detectChanges();
  }
});
