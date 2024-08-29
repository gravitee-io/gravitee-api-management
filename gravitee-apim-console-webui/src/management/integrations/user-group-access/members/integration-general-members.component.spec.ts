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
import { HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { of } from 'rxjs';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IntegrationGeneralMembersComponent } from './integration-general-members.component';
import { IntegrationGeneralMembersHarness } from './integration-general-members.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { IntegrationUserGroupModule } from '../integration-user-group.module';
import { RoleService } from '../../../../services-ngx/role.service';
import { Role } from '../../../../entities/role/role';
import { fakeRole } from '../../../../entities/role/role.fixture';
import { fakeGroup, fakeGroupsResponse, MembersResponse } from '../../../../entities/management-api-v2';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { Integration } from '../../integrations.model';
import { fakeIntegration } from '../../../../entities/integrations/integration.fixture';
import { fakeSearchableUser } from '../../../../entities/user/searchableUser.fixture';

describe('IntegrationGeneralMembersComponent', () => {
  let fixture: ComponentFixture<IntegrationGeneralMembersComponent>;
  let httpTestingController: HttpTestingController;
  let harness: IntegrationGeneralMembersHarness;
  const membersResponse: MembersResponse = {
    data: [
      { id: '1', displayName: 'Mufasa', roles: [{ name: 'King', scope: 'INTEGRATION' }] },
      { id: '2', displayName: 'Simba', roles: [{ name: 'Prince', scope: 'INTEGRATION' }] },
    ],
  };

  const integrationId = 'integrationId';
  const roles: Role[] = [
    fakeRole({ name: 'PRIMARY_OWNER', default: false }),
    fakeRole({ name: 'OWNER', default: false }),
    fakeRole({ name: 'USER', default: true }),
  ];

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, MatIconTestingModule, IntegrationUserGroupModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { integrationId: integrationId } } } },
        { provide: RoleService, useValue: { list: () => of(roles) } },
        {
          provide: GioTestingPermissionProvider,
          useValue: ['environment-integration-r', 'environment-integration-u', 'environment-integration-c', 'environment-integration-d'],
        },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This checks focus trap, set it to true to  avoid the warning
        isTabbable: () => true,
      },
    });

    fixture = TestBed.createComponent(IntegrationGeneralMembersComponent);
    httpTestingController = TestBed.inject(HttpTestingController);

    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, IntegrationGeneralMembersHarness);

    fixture.detectChanges();
  });

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('members list', () => {
    it('should call for integration and members', async () => {
      expectIntegrationGetRequest();
      expectIntegrationMembersGetRequest();
      expectGetGroupsListRequest();
    });

    it('should show all integration members with roles', async () => {
      const members: MembersResponse = {
        data: [
          { id: '1', displayName: 'Mufasa', roles: [{ name: 'King', scope: 'INTEGRATION' }] },
          { id: '2', displayName: 'Simba', roles: [{ name: 'Prince', scope: 'INTEGRATION' }] },
        ],
      };

      expectIntegrationGetRequest();
      expectIntegrationMembersGetRequest(members);
      expectGetGroupsListRequest();
      fixture.detectChanges();

      expect((await harness.getTableRows()).length).toEqual(2);
      expect(await harness.getMembersName()).toEqual(['Mufasa', 'Simba']);
    });

    it('should make role readonly when user is PRIMARY OWNER', async () => {
      const members: MembersResponse = {
        data: [{ id: '1', displayName: 'admin', roles: [{ name: 'PRIMARY_OWNER', scope: 'INTEGRATION' }] }],
      };
      expectIntegrationGetRequest();
      expectIntegrationMembersGetRequest(members);
      expectGetGroupsListRequest();
      fixture.detectChanges();

      expect(await harness.isMemberRoleSelectDisabled(0)).toEqual(true);
    });

    it('should call API to change role when clicking on save', async () => {
      const members: MembersResponse = {
        data: [
          { id: '1', displayName: 'owner', roles: [{ name: 'PRIMARY_OWNER', scope: 'INTEGRATION' }] },
          {
            id: '2',
            displayName: 'other',
            roles: [{ name: 'OWNER', scope: 'INTEGRATION' }],
          },
        ],
      };
      expectIntegrationGetRequest();
      expectIntegrationMembersGetRequest(members);
      expectGetGroupsListRequest();
      fixture.detectChanges();

      await harness.getMemberRoleSelectForRowIndex(1).then(async (select) => {
        await select.open();
        return await select.clickOptions({ text: 'USER' });
      });

      await harness.clickOnSave();

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integrationId}/members/2`,
        method: 'PUT',
      });
      req.flush({});
      expect(req.request.body).toEqual({ memberId: '2', roleName: 'USER' });

      expectIntegrationGetRequest();
      expectIntegrationMembersGetRequest(members);
      expectGetGroupsListRequest();
      fixture.detectChanges();
    });

    it('should make role not editable when user is set to PRIMARY OWNER', async () => {
      const members: MembersResponse = { data: [{ id: '1', displayName: 'owner', roles: [{ name: 'OWNER', scope: 'INTEGRATION' }] }] };
      expectIntegrationGetRequest();
      expectIntegrationMembersGetRequest(members);
      expectGetGroupsListRequest();
      fixture.detectChanges();

      const isRoleDisabled = await harness.isMemberRoleSelectDisabled(0);
      expect(isRoleDisabled).toEqual(false);

      const roleOptions = await harness.getMemberRoleSelectOptions(0);
      const options = await Promise.all(roleOptions.map(async (opt) => await opt.getText()));
      expect(options).toEqual(['PRIMARY_OWNER', 'OWNER', 'USER']);

      const poOption = await harness.getMemberRoleSelectOptions(0, { text: 'PRIMARY_OWNER' });
      expect(poOption.length).toEqual(1);
      expect(await poOption[0].isDisabled()).toEqual(true);
    });

    it('should not allow to delete primary owner', async () => {
      const members: MembersResponse = {
        data: [
          {
            id: '1',
            displayName: 'owner',
            roles: [{ name: 'PRIMARY_OWNER', scope: 'INTEGRATION' }],
          },
        ],
      };

      expectIntegrationGetRequest();
      expectIntegrationMembersGetRequest(members);
      expectGetGroupsListRequest();
      fixture.detectChanges();

      expect(await harness.isMemberDeleteButtonVisible(0)).toEqual(false);
    });

    it('should ask confirmation before delete member, and do nothing if canceled', async () => {
      const members: MembersResponse = {
        data: [
          { id: '1', displayName: 'owner', roles: [{ name: 'PRIMARY_OWNER', scope: 'INTEGRATION' }] },
          {
            id: '2',
            displayName: 'user',
            roles: [{ name: 'USER', scope: 'INTEGRATION' }],
          },
        ],
      };

      expectIntegrationGetRequest();
      expectIntegrationMembersGetRequest(members);
      expectGetGroupsListRequest();
      fixture.detectChanges();

      const isDeleteVisible = await harness.isMemberDeleteButtonVisible(1);

      expect(isDeleteVisible).toEqual(true);

      const deleteBtn = await harness.getMemberDeleteButton(1);
      await deleteBtn.click();

      const confirmDialog = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
      expect(confirmDialog).toBeDefined();

      await confirmDialog.cancel();

      expect((await harness.getTableRows()).length).toEqual(2);
    });

    it('should call the API if member deletion is confirmed', async () => {
      const members: MembersResponse = {
        data: [
          { id: '1', displayName: 'owner', roles: [{ name: 'PRIMARY_OWNER', scope: 'INTEGRATION' }] },
          {
            id: '2',
            displayName: 'user',
            roles: [{ name: 'USER', scope: 'INTEGRATION' }],
          },
        ],
      };

      expectIntegrationGetRequest();
      expectIntegrationMembersGetRequest(members);
      expectGetGroupsListRequest();
      fixture.detectChanges();

      await harness.getMemberDeleteButton(1).then((btn) => btn.click());

      const confirmDialog = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
      expect(confirmDialog).toBeDefined();

      await confirmDialog.confirm();
      expectDeleteMember(integrationId, '2');

      expect((await harness.getTableRows()).length).toEqual(1);
      expect(await harness.getMembersName()).toEqual(['owner']);
    });

    it('should add add members with default role', async () => {
      const members: MembersResponse = {
        data: [
          {
            id: '1',
            displayName: 'Existing User',
            roles: [{ name: 'PRIMARY_OWNER', scope: 'INTEGRATION' }],
          },
        ],
      };

      expectIntegrationGetRequest();
      expectIntegrationMembersGetRequest(members);
      expectGetGroupsListRequest();
      fixture.detectChanges();

      const member = fakeSearchableUser({ id: 'user', displayName: 'User Id' });
      await harness.addMember(member, httpTestingController);

      const membersNames = await harness.getMembersName();
      expect(membersNames).toEqual(['Existing User', 'User Id']);

      // Expect default role to be selected
      const roleOptions = await harness.getMemberRoleSelectOptions(1);
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

      await harness.clickOnSave();

      expectIntegrationMembersPostRequest(member);
    });

    it('should add add members without id', async () => {
      const members: MembersResponse = {
        data: [{ id: '1', displayName: 'Existing User', roles: [{ name: 'PRIMARY_OWNER', scope: 'INTEGRATION' }] }],
      };
      expectIntegrationGetRequest();
      expectIntegrationMembersGetRequest(members);
      expectGetGroupsListRequest();
      fixture.detectChanges();

      const memberToAdd = fakeSearchableUser({ id: undefined, displayName: 'User from LDAP' });
      await harness.addMember(memberToAdd, httpTestingController);

      expect(await harness.getMembersName()).toEqual(['Existing User', 'User from LDAP']);

      await harness.clickOnSave();
      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integrationId}/members`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({ userId: undefined, externalReference: memberToAdd.reference, roleName: 'USER' });
    });
  });

  function expectIntegrationGetRequest(integrationMock: Integration = fakeIntegration()): void {
    const req: TestRequest = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integrationId}`);
    req.flush(integrationMock);
  }

  function expectIntegrationMembersGetRequest(members: MembersResponse = membersResponse) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integrationId}/members`, method: 'GET' })
      .flush(members);
    fixture.detectChanges();
  }

  function expectIntegrationMembersPostRequest(newMember): void {
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integrationId}/members`,
      method: 'POST',
    });
    expect(req.request.body).toEqual({ userId: newMember.id, externalReference: newMember.reference, roleName: 'USER' });
  }

  function expectGetGroupsListRequest(groups: string[] = ['test9999']) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups?page=1&perPage=9999`, method: 'GET' })
      .flush(fakeGroupsResponse({ data: groups.map((id) => fakeGroup({ id, name: id + '-name' })) }));
    fixture.detectChanges();
  }

  function expectDeleteMember(integrationId: string, memberId: string) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integrationId}/members/${memberId}`,
        method: 'DELETE',
      })
      .flush({});
    fixture.detectChanges();
  }
});
