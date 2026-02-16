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
import { ActivatedRoute } from '@angular/router';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

import { ClusterTransferOwnershipDialogHarness } from './transfer-ownership/cluster-transfer-ownership-dialog.harness';
import { ClusterUserPermissionsHarness } from './cluster-user-permissions.harness';
import { ClusterUserPermissionsComponent } from './cluster-user-permissions.component';

import { GioTestingModule, CONSTANTS_TESTING } from '../../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { fakeRole } from '../../../../entities/role/role.fixture';
import { fakeBaseGroup, fakeCluster, fakeGroupsResponse, GroupsResponse, MembersResponse } from '../../../../entities/management-api-v2';
import { GioUsersSelectorHarness } from '../../../../shared/components/gio-users-selector/gio-users-selector.harness';
import { fakeSearchableUser } from '../../../../entities/user/searchableUser.fixture';
import { expectGetClusterRequest } from '../../../../services-ngx/cluster.service.spec';
import { fakeMember } from '../../../../entities/management-api-v2/member/member.fixture';

describe('ClusterUserPermissionsComponent', () => {
  const CLUSTER_ID = 'cluster-id';

  let fixture: ComponentFixture<ClusterUserPermissionsComponent>;
  let componentHarness: ClusterUserPermissionsHarness;
  let httpTestingController: HttpTestingController;
  let rootHarness: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, MatIconTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { clusterId: CLUSTER_ID } } } },
        {
          provide: GioTestingPermissionProvider,
          useValue: ['cluster-member-u', 'cluster-member-c', 'cluster-member-d', 'cluster-definition-u'],
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true,
          isTabbable: () => true,
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ClusterUserPermissionsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ClusterUserPermissionsHarness);
    rootHarness = TestbedHarnessEnvironment.documentRootLoader(fixture);

    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify({ ignoreCancelled: true });
  });

  it('should list members with roles', async () => {
    // Arrange initial requests
    expectInitRequests({
      members: {
        data: [
          { id: '1', displayName: 'Mufasa', roles: [{ name: 'PRIMARY_OWNER', scope: 'CLUSTER' }] },
          { id: '2', displayName: 'Simba', roles: [{ name: 'USER', scope: 'CLUSTER' }] },
        ],
        pagination: { totalCount: 2 },
      },
    });

    expect(await componentHarness.getMembersRowCount()).toBe(2);
    const names = await componentHarness.getDisplayedMemberNames();
    expect(names).toEqual(['Mufasa', 'Simba']);

    // Primary owner role cannot be changed
    const selectRow0 = await componentHarness.getRoleSelectForRow(0);
    expect(await selectRow0.getValueText()).toEqual('PRIMARY_OWNER');
    expect(await selectRow0.isDisabled()).toEqual(true);

    // Other roles can be changed
    const selectRow1 = await componentHarness.getRoleSelectForRow(1);
    expect(await selectRow1.getValueText()).toEqual('USER');
    await selectRow1.open();
    const options = await selectRow1.getOptions();
    const optionTexts = await Promise.all(options.map(o => o.getText()));
    expect(optionTexts).toEqual(['PRIMARY_OWNER', 'OWNER', 'USER']);
  });

  it('should change a role and save', async () => {
    // Arrange initial requests
    expectInitRequests({
      members: {
        data: [{ id: '1', displayName: 'Mufasa', roles: [{ name: 'USER', scope: 'CLUSTER' }] }],
        pagination: { totalCount: 1 },
      },
    });

    expect(await componentHarness.getMembersRowCount()).toEqual(1);

    // Change role of the only member
    await componentHarness.selectRoleForRow(0, 'OWNER');
    await componentHarness.clickSave();

    // Expect PUT to update role
    const putReq = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/clusters/${CLUSTER_ID}/members/1`,
      method: 'PUT',
    });
    expect(putReq.request.body).toEqual({ roleName: 'OWNER', memberId: '1' });
    putReq.flush({});

    // Then component re-inits: expect the sequence again
    expectInitRequests();
  });

  it('should list groups inherited members', async () => {
    // Arrange initial requests
    expectInitRequests({
      groups: fakeGroupsResponse({
        data: [
          fakeBaseGroup({ id: 'g1', name: 'Group 1' }),
          fakeBaseGroup({ id: 'g2', name: 'Group 2' }),
          fakeBaseGroup({ id: 'g3', name: 'Group 3' }),
        ],
      }),
    });
    // For each group, expect a get members request
    expectGetGroupMembersRequest('g1', {
      data: [fakeMember({ id: '1', displayName: 'Mufasa', roles: [{ name: 'USER', scope: 'CLUSTER' }] })],
      pagination: { totalCount: 1 },
    });
    expectGetGroupMembersRequest('g2', {
      data: [],
      pagination: { totalCount: 0 },
    });
    expectGetGroupMembersRequest('g3', {
      data: [fakeMember({ id: '3', displayName: 'Scar', roles: [{ name: 'USER', scope: 'CLUSTER' }] })],
      pagination: { totalCount: 1 },
    });

    const group1MembersHarness = await componentHarness.getGroupMembersHarness('g1');
    expect(await group1MembersHarness.getGroupTitle()).toEqual('Group Group 1 members (1)');
    expect(await group1MembersHarness.getGroupMembersNamesAndRoles()).toEqual([{ name: 'Mufasa', role: 'USER' }]);
    // No GroupMembers (table/card) expected if no members
    const group2MembersHarness = await componentHarness.getGroupMembersHarness('g2');
    expect(await group2MembersHarness.isGroupMembersDisplayed()).toEqual(false);

    const group3MembersHarness = await componentHarness.getGroupMembersHarness('g3');
    expect(await group3MembersHarness.getGroupTitle()).toEqual('Group Group 3 members (1)');
    expect(await group3MembersHarness.getGroupMembersNamesAndRoles()).toEqual([{ name: 'Scar', role: 'USER' }]);
  });

  it('should add a member and save', async () => {
    expectInitRequests({
      members: {
        data: [{ id: '1', displayName: 'Existing User', roles: [{ name: 'PRIMARY_OWNER', scope: 'CLUSTER' }] }],
        pagination: { totalCount: 1 },
      },
    });

    await componentHarness.clickAddMembers();
    fixture.detectChanges();

    // Use users selector to search and pick a user
    const usersSelector = await rootHarness.getHarness(GioUsersSelectorHarness);
    await usersSelector.typeSearch('flash');
    await fixture.whenStable();

    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.org.baseURL}/search/users?q=flash`)
      .flush([fakeSearchableUser({ id: 'userToAdd', displayName: 'User to add' })]);
    await usersSelector.selectUser('User to add');
    await usersSelector.validate();

    // Expect the UI to have 2 rows now
    expect(await componentHarness.getMembersRowCount()).toEqual(2);

    // Save
    await componentHarness.clickSave();

    // Expect POST to add member with default role USER
    const postReq = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/clusters/${CLUSTER_ID}/members`,
      method: 'POST',
    });
    expect(postReq.request.body).toEqual({ userId: 'userToAdd', externalReference: expect.anything(), roleName: 'USER' });
    postReq.flush({});

    // Then component re-inits
    expectInitRequests();
  });

  it('should transfer ownership to an existing member using the dialog harness', async () => {
    // Arrange initial requests with a PRIMARY_OWNER and another USER
    expectInitRequests({
      members: {
        data: [
          { id: '1', displayName: 'Mufasa', roles: [{ name: 'PRIMARY_OWNER', scope: 'CLUSTER' }] },
          { id: '2', displayName: 'Simba', roles: [{ name: 'USER', scope: 'CLUSTER' }] },
        ],
        pagination: { totalCount: 2 },
      },
    });

    // Open the transfer ownership dialog
    const transferBtn = await rootHarness.getHarness(MatButtonHarness.with({ text: /Transfer ownership/i }));
    await transferBtn.click();

    // Interact with the dialog using its harness
    const dialog = await rootHarness.getHarness(ClusterTransferOwnershipDialogHarness);

    // Default mode is ENTITY_MEMBER; select the member 'Simba' and choose new role for current PO
    await dialog.selectEntityMemberByText('Simba');
    await dialog.selectRoleByText('OWNER');

    // Submit the dialog
    expect(await dialog.isSubmitEnabled()).toBe(true);
    await dialog.submit();

    // Expect POST to transfer ownership endpoint
    const transferReq = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/clusters/${CLUSTER_ID}/members/_transfer-ownership`,
      method: 'POST',
    });
    expect(transferReq.request.body).toEqual(expect.objectContaining({ newPrimaryOwnerId: '2', currentPrimaryOwnerNewRole: 'OWNER' }));
    transferReq.flush({});

    // Then component re-inits: expect the sequence again
    expectInitRequests();
  });

  function expectInitRequests({
    members = { data: [], pagination: { totalCount: 0 } },
    groups = fakeGroupsResponse({ data: [] }),
  }: {
    members?: MembersResponse;
    groups?: GroupsResponse;
  } = {}) {
    // GET members
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/clusters/${CLUSTER_ID}/members?page=1&perPage=10`,
        method: 'GET',
      })
      .flush(members);

    // GET roles
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/CLUSTER/roles`, method: 'GET' })
      .flush([
        fakeRole({ name: 'PRIMARY_OWNER', default: false, scope: 'CLUSTER' }),
        fakeRole({ name: 'OWNER', default: false, scope: 'CLUSTER' }),
        fakeRole({ name: 'USER', default: true, scope: 'CLUSTER' }),
      ]);

    // GET cluster
    const groupsIds = groups.data.map(g => g.id);
    expectGetClusterRequest(httpTestingController, fakeCluster({ id: CLUSTER_ID, groups: groupsIds }));

    // GET groups list
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups?page=1&perPage=9999`, method: 'GET' }).flush(groups);

    fixture.detectChanges();
  }

  function expectGetGroupMembersRequest(groupId: string, members: MembersResponse) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups/${groupId}/members?page=1&perPage=10`,
        method: 'GET',
      })
      .flush(members);
  }
});
