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
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ActivatedRoute } from '@angular/router';
import { HarnessLoader } from '@angular/cdk/testing';

import { IntegrationGeneralGroupsHarness } from './integration-general-groups.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { IntegrationUserGroupModule } from '../integration-user-group.module';
import { Api, fakeApiV1, fakeApiV4, fakeGroup, fakeGroupsResponse, Group, MembersResponse } from '../../../../entities/management-api-v2';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { IntegrationGeneralMembersComponent } from '../members/integration-general-members.component';
import { IntegrationGeneralMembersHarness } from '../members/integration-general-members.harness';
import { Role } from '../../../../entities/role/role';
import { fakeRole } from '../../../../entities/role/role.fixture';

xdescribe('IntegrationPortalGroupsComponent', () => {
  const apiId = 'api-id';
  const groupId1 = 'group-1';
  const groupId2 = 'group-2';
  const defaultRoles: Role[] = [
    fakeRole({ name: 'PRIMARY_OWNER', default: false }),
    fakeRole({ name: 'OWNER', default: false }),
    fakeRole({ name: 'USER', default: true }),
  ];

  let fixture: ComponentFixture<IntegrationGeneralMembersComponent>;
  let harness: IntegrationGeneralMembersHarness;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const init = async (permissions: string[]) => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, IntegrationUserGroupModule, MatIconTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: apiId } } } },
        { provide: GioTestingPermissionProvider, useValue: permissions },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(IntegrationGeneralMembersComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, IntegrationGeneralMembersHarness);

    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('Groups tab for user with writing rights', () => {
    beforeEach(async () => {
      await init(['api-definition-u', 'api-member-u']);
    });

    it('should show groups', async () => {
      const api = fakeApiV4({ id: apiId, groups: [] });
      await expectGetRequests(api, [groupId1, groupId2]);
      expect(await harness.getGroupsLength()).toStrictEqual(api.groups.length);
      expect(await harness.getGroupsNames()).toEqual(api.groups.map((id) => `Group ${id}-name`));
      api.groups.forEach((id) => expectGetGroupMembersRequest(fakeGroup({ id })));

      await harness.manageGroupsClick();
      const groupsHarness = await rootLoader.getHarness(IntegrationGeneralGroupsHarness);
      expect(await groupsHarness.getFillFormLabel()).toEqual('Groups');
      expect(await groupsHarness.isReadOnlyGroupsPresent()).toEqual(false);

      await groupsHarness.selectGroups({ text: `${groupId2}-name` });
      expect(await groupsHarness.getGroupsListValueText()).toEqual(`${groupId2}-name`);
      expect(await groupsHarness.isReadOnlyGroupsPresent()).toEqual(false);
    });

    it('should pre-select groups found in user + save new groups', async () => {
      const api = fakeApiV4({ id: apiId, groups: [groupId1] });
      await expectGetRequests(api, [groupId1, groupId2]);
      api.groups.forEach((id) => expectGetGroupMembersRequest(fakeGroup({ id })));

      await harness.manageGroupsClick();
      const groupsHarness = await rootLoader.getHarness(IntegrationGeneralGroupsHarness);
      expect(await groupsHarness.isFillFormControlDirty()).toEqual(false);

      const allGroups = await groupsHarness.getGroups();
      expect(allGroups.length).toEqual(2);

      const selectedGroups = await groupsHarness.getSelectedGroups();
      expect(selectedGroups.length).toEqual(1);
      expect(await selectedGroups[0].getText()).toEqual(`${groupId1}-name`);

      await groupsHarness.selectGroups({ text: `${groupId2}-name` });
      expect(await groupsHarness.getGroupsListValueText()).toEqual(`${groupId1}-name, ${groupId2}-name`);
      await groupsHarness.closeGroupsList();

      expect(await groupsHarness.isFillFormControlDirty()).toEqual(true);
      expect(await groupsHarness.isSaveButtonVisible()).toEqual(true);
      expect(await groupsHarness.isSaveButtonDisabled()).toEqual(false);

      await groupsHarness.clickSave();

      // expect save and after that reload of the component.
      // MatDialog close is not called in the unit test context, so we do the PUT request with the real saved data
      expectApiGetRequest(api);
      expectApiPutRequest({ ...api, groups: [] });
      expectGetRequests({ ...api, groups: [] });
    });

    it('should be read-only for V1 API', async () => {
      const api = fakeApiV1({ id: apiId, groups: [groupId1] });
      await expectGetRequests(api, [groupId1]);
      expectGetGroupMembersRequest(fakeGroup({ id: groupId1 }));

      await harness.manageGroupsClick();

      const groupsHarness = await rootLoader.getHarness(IntegrationGeneralGroupsHarness);
      expect(await groupsHarness.isReadOnlyGroupsPresent()).toEqual(true);
      expect(await groupsHarness.getReadOnlyGroupsText()).toContain(`${groupId1}-name`);
      expect(await groupsHarness.isFillFormPresent()).toEqual(false);
      expect(await groupsHarness.isSaveButtonVisible()).toEqual(false);
    });
  });

  describe('Groups tab for user with read-only rights', () => {
    beforeEach(async () => {
      await init(['api-definition-r', 'api-member-r', 'api-gateway_definition-u']);
    });

    it('should display list of groups', async () => {
      const api = fakeApiV4({ id: apiId, groups: [groupId1, groupId2] });
      await expectGetRequests(api, [groupId1, groupId2]);

      expect(await harness.getGroupsNames()).toEqual(api.groups.map((id) => `Group ${id}-name`));
      expect(await harness.getGroupsLength()).toStrictEqual(api.groups.length);
      api.groups.forEach((id) => expectGetGroupMembersRequest(fakeGroup({ id })));

      await harness.manageGroupsClick();

      const groupsHarness = await rootLoader.getHarness(IntegrationGeneralGroupsHarness);
      expect(await groupsHarness.isReadOnlyGroupsPresent()).toEqual(true);
      expect(await groupsHarness.getReadOnlyGroupsText()).toContain(`${groupId1}-name, ${groupId2}-name`);
      expect(await groupsHarness.isFillFormPresent()).toEqual(false);
      expect(await groupsHarness.isSaveButtonVisible()).toEqual(false);
    });
  });

  async function expectGetRequests(api: Api, groups: string[] = [], members: MembersResponse = { data: [] }) {
    expectApiGetRequest(api);
    expectGetGroupsListRequest(groups);
    expectApiMembersGetRequest(api, members);
    expectApiRoleGetRequest();
  }

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  function expectApiPutRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'PUT' }).flush(api);
  }

  function expectGetGroupsListRequest(groups: string[]) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups?page=1&perPage=9999`, method: 'GET' })
      .flush(fakeGroupsResponse({ data: groups.map((id) => fakeGroup({ id, name: id + '-name' })) }));
    fixture.detectChanges();
  }

  function expectApiMembersGetRequest(api: Api, members: MembersResponse = { data: [] }) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}/members`, method: 'GET' }).flush(members);
    fixture.detectChanges();
  }

  function expectGetGroupMembersRequest(group: Group) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups/${group.id}/members?page=1&perPage=10`, method: 'GET' })
      .flush({ data: [], metadata: { groupName: 'group1' }, pagination: {} });
    fixture.detectChanges();
  }

  function expectApiRoleGetRequest(roles: Role[] = defaultRoles) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/API/roles`, method: 'GET' })
      .flush(roles);
    fixture.detectChanges();
  }
});
