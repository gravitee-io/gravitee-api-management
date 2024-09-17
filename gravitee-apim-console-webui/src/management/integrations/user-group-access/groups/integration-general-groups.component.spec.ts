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
import { of } from 'rxjs';

import { IntegrationGeneralGroupsHarness } from './integration-general-groups.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { IntegrationUserGroupModule } from '../integration-user-group.module';
import { fakeGroup, fakeGroupsResponse, Group, MembersResponse } from '../../../../entities/management-api-v2';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { IntegrationGeneralMembersComponent } from '../members/integration-general-members.component';
import { IntegrationGeneralMembersHarness } from '../members/integration-general-members.harness';
import { Role } from '../../../../entities/role/role';
import { fakeRole } from '../../../../entities/role/role.fixture';
import { Integration } from '../../integrations.model';
import { fakeIntegration } from '../../../../entities/integrations/integration.fixture';

describe('IntegrationPortalGroupsComponent', () => {
  const integrationId = 'integration-id';
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
    const permissionService = {
      hasAnyMatching: (permissionGuess: string[]) => permissionGuess.some((guess) => permissions.includes(guess)),
      fetchGroupPermissions: (_) => of(['group-member-r']),
    };
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, IntegrationUserGroupModule, MatIconTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { integrationId: integrationId } } } },
        { provide: GioPermissionService, useValue: permissionService },
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
      await init(['integration-member-c', 'integration-member-r']);
    });

    it('should show groups', async () => {
      const integration = fakeIntegration({ id: integrationId, groups: [] });
      await expectGetRequests(integration, [groupId1, groupId2]);
      expect(await harness.getGroupsLength()).toStrictEqual(integration.groups.length);
      expect(await harness.getGroupsNames()).toEqual(integration.groups.map((id) => `Group ${id}-name`));
      integration.groups.forEach((id) => expectGetGroupMembersRequest(fakeGroup({ id })));

      await harness.manageGroupsClick();
      const groupsHarness = await rootLoader.getHarness(IntegrationGeneralGroupsHarness);
      expect(await groupsHarness.getFillFormLabel()).toEqual('Groups');
      expect(await groupsHarness.isReadOnlyGroupsPresent()).toEqual(false);

      await groupsHarness.selectGroups({ text: `${groupId2}-name` });
      expect(await groupsHarness.getGroupsListValueText()).toEqual(`${groupId2}-name`);
      expect(await groupsHarness.isReadOnlyGroupsPresent()).toEqual(false);
    });

    it('should pre-select groups found in user + save new groups', async () => {
      const integration = fakeIntegration({ id: integrationId, groups: [groupId1] });
      await expectGetRequests(integration, [groupId1, groupId2]);
      integration.groups.forEach((id) => expectGetGroupMembersRequest(fakeGroup({ id })));

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
      expectIntegrationGetRequest(integration);
      expectIntegrationPutRequest({ ...integration, groups: [] });
      expectGetRequests({ ...integration, groups: [] });
    });
  });

  describe('Groups tab for user with read-only rights', () => {
    beforeEach(async () => {
      await init(['integration-member-r']);
    });

    it('should display list of groups', async () => {
      const integration = fakeIntegration({ id: integrationId, groups: [groupId1, groupId2] });
      await expectGetRequests(integration, [groupId1, groupId2]);

      expect(await harness.getGroupsNames()).toEqual(integration.groups.map((id) => `Group ${id}-name`));
      expect(await harness.getGroupsLength()).toStrictEqual(integration.groups.length);
      integration.groups.forEach((id) => expectGetGroupMembersRequest(fakeGroup({ id })));

      await harness.manageGroupsClick();

      const groupsHarness = await rootLoader.getHarness(IntegrationGeneralGroupsHarness);
      expect(await groupsHarness.isReadOnlyGroupsPresent()).toEqual(true);
      expect(await groupsHarness.getReadOnlyGroupsText()).toContain(`${groupId1}-name, ${groupId2}-name`);
      expect(await groupsHarness.isFillFormPresent()).toEqual(false);
      expect(await groupsHarness.isSaveButtonVisible()).toEqual(false);
    });
  });

  async function expectGetRequests(integration: Integration, groups: string[] = [], members: MembersResponse = { data: [] }) {
    expectIntegrationGetRequest(integration);
    expectGetGroupsListRequest(groups);
    expectIntegrationMembersGetRequest(integration, members);
    expectIntegrationRoleGetRequest();
  }

  function expectIntegrationGetRequest(integration: Integration) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integration.id}`,
        method: 'GET',
      })
      .flush(integration);
    fixture.detectChanges();
  }

  function expectIntegrationPutRequest(integration: Integration) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integration.id}`,
        method: 'PUT',
      })
      .flush(integration);
  }

  function expectGetGroupsListRequest(groups: string[]) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups?page=1&perPage=9999`, method: 'GET' })
      .flush(fakeGroupsResponse({ data: groups.map((id) => fakeGroup({ id, name: id + '-name' })) }));
    fixture.detectChanges();
  }

  function expectIntegrationMembersGetRequest(integration: Integration, members: MembersResponse = { data: [] }) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integration.id}/members`,
        method: 'GET',
      })
      .flush(members);
    fixture.detectChanges();
  }

  function expectGetGroupMembersRequest(group: Group) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups/${group.id}/members?page=1&perPage=10`,
        method: 'GET',
      })
      .flush({ data: [], metadata: { groupName: 'group1' }, pagination: {} });
    fixture.detectChanges();
  }

  function expectIntegrationRoleGetRequest(roles: Role[] = defaultRoles) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/INTEGRATION/roles`,
        method: 'GET',
      })
      .flush(roles);
    fixture.detectChanges();
  }
});
