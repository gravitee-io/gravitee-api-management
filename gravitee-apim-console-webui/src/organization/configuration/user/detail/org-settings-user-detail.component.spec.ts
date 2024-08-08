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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatCardHarness } from '@angular/material/card/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { By } from '@angular/platform-browser';

import { OrgSettingsUserDetailComponent } from './org-settings-user-detail.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { OrganizationSettingsModule } from '../../organization-settings.module';
import { fakeUser } from '../../../../entities/user/user.fixture';
import { Role } from '../../../../entities/role/role';
import { fakeRole } from '../../../../entities/role/role.fixture';
import { Environment } from '../../../../entities/environment/environment';
import { fakeEnvironment } from '../../../../entities/environment/environment.fixture';
import { Group } from '../../../../entities/group/group';
import { fakeGroup } from '../../../../entities/group/group.fixture';
import { GroupMembership } from '../../../../entities/group/groupMember';
import { fakeGroupMembership } from '../../../../entities/group/groupMember.fixture';
import { fakeUserMembership } from '../../../../entities/user/userMembership.fixture';
import { UserMembership } from '../../../../entities/user/userMembership';
import { GioTableWrapperHarness } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.harness';
import { Token } from '../../../../entities/user/userTokens';
import { fakeUserToken } from '../../../../entities/user/userToken.fixture';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { User } from '../../../../entities/user/user';

describe('OrgSettingsUserDetailComponent', () => {
  let fixture: ComponentFixture<OrgSettingsUserDetailComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, OrganizationSettingsModule, MatIconTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { userId: 'userId' } }, fragment: of('') } },
        {
          provide: GioTestingPermissionProvider,
          useValue: ['organization-user-u', 'organization-user-d', 'organization-user-c'],
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This checks focus trap, set it to true to  avoid the warning
          isTabbable: () => true, // This checks tabbable trap, set it to true to  avoid the warning
        },
      })
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(OrgSettingsUserDetailComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
    jest.resetAllMocks();
  });

  it('should display user details', async () => {
    const customFields = {
      'custom-field-1': 'custom-value-1',
    };
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      roles: [
        { id: 'ROLE_USER', name: 'ROLE_USER', scope: 'ORGANIZATION' },
        { id: 'ROLE_USER', name: 'ROLE_USER', scope: 'API' },
      ],
      customFields,
    });
    expectUserTokensGetRequest(user);
    expectUserGetRequest(user);
    expectEnvironmentListRequest();
    expectUserGroupsGetRequest(user.id);
    expectUserMembershipGetRequest(user.id, 'api');
    expectUserMembershipGetRequest(user.id, 'application');
    expectRolesListRequest('ORGANIZATION', [
      fakeRole({ id: 'roleOrgUserId', name: 'ROLE_ORG_USER' }),
      fakeRole({ id: 'roleOrgAdminId', name: 'ROLE_ORG_ADMIN' }),
    ]);

    const userCard = await loader.getHarness(MatCardHarness);

    [user.displayName, user.email, customFields['custom-field-1'], 'ROLE_USER'].forEach(async (value) => {
      expect(await userCard.getText()).toContain(value);
    });

    const userStatus = await userCard.getText();
    expect(await userStatus).toContain('Active');

    const resetPasswordButton = await userCard.getHarness(MatButtonHarness.with({ text: 'Reset password' }));
    expect(resetPasswordButton).toBeTruthy();
  });

  it('should reset password after confirm dialog', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
    });
    expectUserTokensGetRequest(user);
    expectUserGetRequest(user);
    expectEnvironmentListRequest();
    expectUserGroupsGetRequest(user.id);
    expectUserMembershipGetRequest(user.id, 'api');
    expectUserMembershipGetRequest(user.id, 'application');
    expectRolesListRequest('ORGANIZATION');

    const userCard = await loader.getHarness(MatCardHarness.with({ selector: '.org-settings-user-detail__card' }));
    const resetButton = await userCard.getHarness(MatButtonHarness.with({ text: 'Reset password' }));

    await resetButton.click();

    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (await dialog.getHarness(MatButtonHarness.with({ text: 'Reset' }))).click();

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users/${user.id}/resetPassword`);
    expect(req.request.method).toEqual('POST');
    // No flush to stop test here
  });

  it('should not reset password if no firstname', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      email: null,
      firstname: null,
    });
    expectUserTokensGetRequest(user);
    expectUserGetRequest(user);
    expectEnvironmentListRequest();
    expectUserGroupsGetRequest(user.id);
    expectUserMembershipGetRequest(user.id, 'api');
    expectUserMembershipGetRequest(user.id, 'application');
    expectRolesListRequest('ORGANIZATION');

    const userCard = await loader.getHarness(MatCardHarness.with({ selector: '.org-settings-user-detail__card' }));
    expect(await userCard.getAllHarnesses(MatButtonHarness.with({ text: 'Reset password' }))).toHaveLength(0);
  });

  it('should accept user registration after confirm dialog', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'PENDING',
    });
    expectUserTokensGetRequest(user);
    expectUserGetRequest(user);
    expectEnvironmentListRequest();
    expectUserGroupsGetRequest(user.id);
    expectUserMembershipGetRequest(user.id, 'api');
    expectUserMembershipGetRequest(user.id, 'application');
    expectRolesListRequest('ORGANIZATION');

    const userCard = await loader.getHarness(MatCardHarness.with({ selector: '.org-settings-user-detail__card' }));
    const acceptUserRegistrationButton = await userCard.getHarness(
      MatButtonHarness.with({ selector: '[aria-label="Accept user registration"]' }),
    );

    await acceptUserRegistrationButton.click();

    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (await dialog.getHarness(MatButtonHarness.with({ text: 'Accept' }))).click();

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users/${user.id}/_process`);
    expect(req.request.method).toEqual('POST');
    expect(req.request.body).toEqual(true);
    // No flush to stop test here
  });

  it('should reject user registration after confirm dialog', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'PENDING',
    });
    expectUserTokensGetRequest(user);
    expectUserGetRequest(user);
    expectEnvironmentListRequest();
    expectUserGroupsGetRequest(user.id);
    expectUserMembershipGetRequest(user.id, 'api');
    expectUserMembershipGetRequest(user.id, 'application');
    expectRolesListRequest('ORGANIZATION');

    const userCard = await loader.getHarness(MatCardHarness.with({ selector: '.org-settings-user-detail__card' }));
    const acceptUserRegistrationButton = await userCard.getHarness(
      MatButtonHarness.with({ selector: '[aria-label="Reject user registration"]' }),
    );

    await acceptUserRegistrationButton.click();

    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (await dialog.getHarness(MatButtonHarness.with({ text: 'Reject' }))).click();

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users/${user.id}/_process`);
    expect(req.request.method).toEqual('POST');
    expect(req.request.body).toEqual(false);
    // No flush to stop test here
  });

  it('should display registration banner', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'PENDING',
    });
    expectUserTokensGetRequest(user);
    expectUserGetRequest(user);
    expectEnvironmentListRequest();
    expectUserGroupsGetRequest(user.id);
    expectUserMembershipGetRequest(user.id, 'api');
    expectUserMembershipGetRequest(user.id, 'application');
    expectRolesListRequest('ORGANIZATION');

    const AcceptUserButton = await loader.getHarness(MatButtonHarness.with({ text: /Accept/ }));
    expect(AcceptUserButton).toBeTruthy();
    const RejectUserButton = await loader.getHarness(MatButtonHarness.with({ text: /Reject/ }));
    expect(RejectUserButton).toBeTruthy();
  });

  it('should not display registration banner', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'ACTIVE',
    });
    expectUserTokensGetRequest(user);
    expectUserGetRequest(user);
    expectEnvironmentListRequest();
    expectUserGroupsGetRequest(user.id);
    expectUserMembershipGetRequest(user.id, 'api');
    expectUserMembershipGetRequest(user.id, 'application');
    expectRolesListRequest('ORGANIZATION');

    expect(await loader.getAllHarnesses(MatButtonHarness.with({ text: /Accept/ }))).toHaveLength(0);
    expect(await loader.getAllHarnesses(MatButtonHarness.with({ text: /Reject/ }))).toHaveLength(0);
  });

  it('should save organization roles', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'ACTIVE',
      roles: [{ id: 'roleOrgUserId', name: 'ROLE_ORG_USER', scope: 'ORGANIZATION' }],
    });
    expectUserTokensGetRequest(user);
    expectUserGetRequest(user);
    expectEnvironmentListRequest();
    expectUserGroupsGetRequest(user.id);
    expectUserMembershipGetRequest(user.id, 'api');
    expectUserMembershipGetRequest(user.id, 'application');
    expectRolesListRequest('ORGANIZATION', [
      fakeRole({ id: 'roleOrgUserId', name: 'ROLE_ORG_USER' }),
      fakeRole({ id: 'roleOrgAdminId', name: 'ROLE_ORG_ADMIN' }),
    ]);

    const orgRoleCard = await loader.getHarness(MatCardHarness.with({ selector: '.org-settings-user-detail__org-role-card' }));
    const rolesSelect = await orgRoleCard.getHarness(MatSelectHarness);
    await rolesSelect.open();
    await rolesSelect.clickOptions({ text: 'ROLE_ORG_ADMIN' });

    expect(await rolesSelect.getValueText()).toBe('ROLE_ORG_USER, ROLE_ORG_ADMIN');

    const saveBar = await loader.getHarness(GioSaveBarHarness);
    await saveBar.clickSubmit();

    expectUpdateUserRolesRequest(user.id, 'organization-id', 'ORGANIZATION', ['roleOrgUserId', 'roleOrgAdminId']);
  });

  it('should save environments roles', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'ACTIVE',
      envRoles: { environmentAlphaId: [{ id: 'roleEnvApiId' }], environmentBetaId: [] },
    });
    expectUserTokensGetRequest(user);
    expectUserGetRequest(user);
    expectUserGroupsGetRequest(user.id);
    expectUserMembershipGetRequest(user.id, 'api');
    expectUserMembershipGetRequest(user.id, 'application');
    expectEnvironmentListRequest([
      fakeEnvironment({ id: 'environmentAlphaId', name: 'Environment Alpha' }),
      fakeEnvironment({ id: 'environmentBetaId', name: 'Environment Beta' }),
    ]);
    expectRolesListRequest('ENVIRONMENT', [
      fakeRole({ id: 'roleEnvApiId', name: 'ROLE_ENV_API' }),
      fakeRole({ id: 'roleEnvUserId', name: 'ROLE_ENV_USER' }),
    ]);
    expectRolesListRequest('ORGANIZATION');

    const environmentsCard = await loader.getHarness(MatCardHarness.with({ selector: '.org-settings-user-detail__environments-card' }));
    const environmentAlphaRolesSelect = await environmentsCard.getHarness(MatSelectHarness.with({ selector: '[id=environmentAlphaId]' }));

    await environmentAlphaRolesSelect.clickOptions({ text: 'ROLE_ENV_USER' });

    expect(await environmentAlphaRolesSelect.getValueText()).toBe('ROLE_ENV_API, ROLE_ENV_USER');

    const saveBar = await loader.getHarness(GioSaveBarHarness);
    await saveBar.clickSubmit();

    expectUpdateUserRolesRequest(user.id, 'environmentAlphaId', 'ENVIRONMENT', ['roleEnvApiId', 'roleEnvUserId']);
  });

  it('should save group roles', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'ACTIVE',
    });
    expectUserTokensGetRequest(user);
    expectUserGetRequest(user);
    expectEnvironmentListRequest();
    expectUserGroupsGetRequest(user.id, [
      fakeGroup({
        id: 'groupA',
        roles: { GROUP: 'ADMIN', API: 'ROLE_API', APPLICATION: 'ROLE_APP_OWNER', INTEGRATION: 'ROLE_INTEGRATION_OWNER' },
      }),
    ]);
    expectUserMembershipGetRequest(user.id, 'api');
    expectUserMembershipGetRequest(user.id, 'application');
    expectRolesListRequest('ORGANIZATION');
    expectRolesListRequest('API', [fakeRole({ id: 'roleApiId', name: 'ROLE_API' })]);
    expectRolesListRequest('APPLICATION', [
      fakeRole({ id: 'roleAppOwnerId', name: 'ROLE_APP_OWNER' }),
      fakeRole({ id: 'roleAppUserId', name: 'ROLE_APP_USER' }),
    ]);
    expectRolesListRequest('INTEGRATION', [fakeRole({ id: 'roleIntegrationOwnerId', name: 'ROLE_INTEGRATION_OWNER' })]);

    const groupsCard = await loader.getHarness(MatCardHarness.with({ selector: '.org-settings-user-detail__groups-card' }));
    const groupsTable = await groupsCard.getHarness(MatTableHarness);

    const groupAdminCheckbox = await (await (await groupsTable.getRows())[0].getCells())[1].getHarness(MatCheckboxHarness);
    const apiRoleSelect = await (await (await groupsTable.getRows())[0].getCells())[2].getHarness(MatSelectHarness);
    const applicationRoleSelect = await (await (await groupsTable.getRows())[0].getCells())[3].getHarness(MatSelectHarness);
    const integrationRoleSelect = await (await (await groupsTable.getRows())[0].getCells())[4].getHarness(MatSelectHarness);

    // expect initial value
    expect(await groupAdminCheckbox.isChecked()).toBe(true);
    expect(await apiRoleSelect.getValueText()).toBe('ROLE_API');
    expect(await applicationRoleSelect.getValueText()).toBe('ROLE_APP_OWNER');
    expect(await integrationRoleSelect.getValueText()).toBe('ROLE_INTEGRATION_OWNER');

    // change values
    await groupAdminCheckbox.uncheck();
    await applicationRoleSelect.clickOptions({ text: 'ROLE_APP_USER' });

    const saveBar = await loader.getHarness(GioSaveBarHarness);
    await saveBar.clickSubmit();

    expectAddOrUpdateGroupMembershipRequest('groupA', [
      fakeGroupMembership({
        id: user.id,
        roles: [{ scope: 'APPLICATION', name: 'ROLE_APP_USER' }],
      }),
    ]);
  });

  it('should delete user from group after confirm dialog', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'PENDING',
    });
    expectUserTokensGetRequest(user);
    expectUserGetRequest(user);
    expectEnvironmentListRequest();
    expectUserGroupsGetRequest(user.id, [fakeGroup({ id: 'groupA', roles: { GROUP: 'ADMIN' } })]);
    expectUserMembershipGetRequest(user.id, 'api');
    expectUserMembershipGetRequest(user.id, 'application');
    expectRolesListRequest('ORGANIZATION');
    expectRolesListRequest('API');
    expectRolesListRequest('APPLICATION');
    expectRolesListRequest('INTEGRATION');

    const groupsCard = await loader.getHarness(MatCardHarness.with({ selector: '.org-settings-user-detail__groups-card' }));
    const groupsTable = await groupsCard.getHarness(MatTableHarness);

    // First row and delete column
    const deleteUserGroupButton = await (await (await groupsTable.getRows())[0].getCells())[5].getHarness(MatButtonHarness);

    await deleteUserGroupButton.click();

    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (await dialog.getHarness(MatButtonHarness.with({ text: 'Delete' }))).click();

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/configuration/groups/groupA/members/${user.id}`);
    expect(req.request.method).toEqual('DELETE');
    // No flush to stop test here
  });

  it('should display APIs user membership', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'ACTIVE',
    });
    expectUserTokensGetRequest(user);
    expectUserGetRequest(user);
    expectEnvironmentListRequest();
    expectUserGroupsGetRequest(user.id);
    expectUserMembershipGetRequest(
      user.id,
      'api',
      fakeUserMembership('api', {
        metadata: {
          apiAlphaId: { name: 'API Alpha', version: '1.0.0', visibility: 'PUBLIC', environmentId: 'DEFAULT' },
          apiBetaId: { name: 'API Beta', version: '42.0.0', visibility: 'PRIVATE', environmentId: 'DEFAULT' },
        },
      }),
    );
    expectUserMembershipGetRequest(user.id, 'application');
    expectRolesListRequest('ORGANIZATION');

    const apiCard = await loader.getHarness(MatCardHarness.with({ selector: '.org-settings-user-detail__apis-card' }));
    const apiTable = await apiCard.getHarness(MatTableHarness);

    expect(await apiTable.getCellTextByIndex()).toEqual([
      ['API Alpha', '1.0.0', 'public Public'],
      ['API Beta', '42.0.0', 'lock Private'],
    ]);
  });

  it('should display applications user membership', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'ACTIVE',
    });
    expectUserTokensGetRequest(user);
    expectUserGetRequest(user);
    expectEnvironmentListRequest();
    expectUserGroupsGetRequest(user.id);
    expectUserMembershipGetRequest(user.id, 'api');
    expectUserMembershipGetRequest(
      user.id,
      'application',
      fakeUserMembership('application', {
        metadata: {
          appFoxId: { name: 'Application Fox', environmentId: 'DEFAULT' },
          appDogId: { name: 'Application Dog', environmentId: 'DEFAULT' },
        },
      }),
    );
    expectRolesListRequest('ORGANIZATION');

    const apiCard = await loader.getHarness(MatCardHarness.with({ selector: '.org-settings-user-detail__applications-card' }));
    const apiTable = await apiCard.getHarness(MatTableHarness);

    expect(await apiTable.getCellTextByIndex()).toEqual([['Application Fox'], ['Application Dog']]);
  });

  it('should filter applications user membership table', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'ACTIVE',
    });
    expectUserTokensGetRequest(user);
    expectUserGetRequest(user);
    expectEnvironmentListRequest();
    expectUserGroupsGetRequest(user.id);
    expectUserMembershipGetRequest(user.id, 'api');
    expectUserMembershipGetRequest(
      user.id,
      'application',
      fakeUserMembership('application', {
        metadata: {
          appFoxId: { name: 'Application Fox', environmentId: 'DEFAULT' },
          appDogId: { name: 'Application Dog', environmentId: 'DEFAULT' },
        },
      }),
    );
    expectRolesListRequest('ORGANIZATION');

    const applicationsCard = await loader.getHarness(MatCardHarness.with({ selector: '.org-settings-user-detail__applications-card' }));
    const applicationTable = await applicationsCard.getHarness(MatTableHarness);
    const apiTableWrapper = await applicationsCard.getHarness(GioTableWrapperHarness);

    expect(await applicationTable.getCellTextByIndex()).toEqual([['Application Fox'], ['Application Dog']]);

    await apiTableWrapper.setSearchValue('fox');
    expect(await applicationTable.getCellTextByIndex()).toEqual([['Application Fox']]);

    await apiTableWrapper.setSearchValue('');
    expect(await applicationTable.getCellTextByIndex()).toEqual([['Application Fox'], ['Application Dog']]);
  });

  it('should add and save group roles', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'ACTIVE',
    });
    expectUserTokensGetRequest(user);
    expectUserGetRequest(user);
    expectEnvironmentListRequest();
    expectUserGroupsGetRequest(user.id, [fakeGroup({ id: 'groupA', name: 'Group A' })]);
    expectUserMembershipGetRequest(user.id, 'api');
    expectUserMembershipGetRequest(user.id, 'application');
    expectRolesListRequest('ORGANIZATION');
    expectRolesListRequest('API');
    expectRolesListRequest('APPLICATION');
    expectRolesListRequest('INTEGRATION');

    const groupsCard = await loader.getHarness(MatCardHarness.with({ selector: '.org-settings-user-detail__groups-card' }));

    const addGroupButton = await groupsCard.getHarness(MatButtonHarness.with({ text: /Add a group/ }));
    await addGroupButton.click();
    expectGroupListByOrganizationRequest([fakeGroup({ id: 'groupA', name: 'Group A' }), fakeGroup({ id: 'groupB', name: 'Group B' })]);
    fixture.detectChanges();
    expectRolesListRequest('API');
    expectRolesListRequest('APPLICATION');
    expectRolesListRequest('INTEGRATION');

    const dialog = await rootLoader.getHarness(MatDialogHarness);

    const groupIdSelect = await dialog.getHarness(MatSelectHarness.with({ selector: '[formControlName=groupId' }));
    await groupIdSelect.open();
    // group A option is filtered
    expect((await groupIdSelect.getOptions()).length).toEqual(1);

    await groupIdSelect.clickOptions({ text: 'Group B' });

    const isAdminSelect = await dialog.getHarness(MatCheckboxHarness.with({ selector: '[formControlName=isAdmin' }));
    await isAdminSelect.check();

    const submitButton = await dialog.getHarness(MatButtonHarness.with({ selector: 'button[type=submit]' }));
    await submitButton.click();

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/configuration/groups/groupB/members`);
    expect(req.request.method).toEqual('POST');
    expect(req.request.body).toEqual([
      {
        id: 'userId',
        roles: [
          { name: 'ADMIN', scope: 'GROUP' },
          { name: null, scope: 'API' },
          { name: null, scope: 'APPLICATION' },
          { name: null, scope: 'INTEGRATION' },
        ],
      },
    ]);
  });

  it('should open dialog to create a token', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'ACTIVE',
    });
    expectUserTokensGetRequest(user);
    expectUserGetRequest(user);
    expectEnvironmentListRequest();
    expectUserGroupsGetRequest(user.id, [
      fakeGroup({
        id: 'groupA',
        roles: { GROUP: 'ADMIN', API: 'ROLE_API', APPLICATION: 'ROLE_APP_OWNER', INTEGRATION: 'ROLE_INTEGRATION_OWNER' },
      }),
    ]);
    expectUserMembershipGetRequest(user.id, 'api');
    expectUserMembershipGetRequest(user.id, 'application');
    expectRolesListRequest('ORGANIZATION');
    expectRolesListRequest('API', [fakeRole({ id: 'roleApiId', name: 'ROLE_API' })]);
    expectRolesListRequest('APPLICATION', [
      fakeRole({ id: 'roleAppOwnerId', name: 'ROLE_APP_OWNER' }),
      fakeRole({ id: 'roleAppUserId', name: 'ROLE_APP_USER' }),
    ]);
    expectRolesListRequest('INTEGRATION', [fakeRole({ id: 'roleApiId', name: 'ROLE_INTEGRATION_OWNER' })]);

    const tokensCard = await loader.getHarness(MatCardHarness.with({ selector: '.org-settings-user-detail__tokens__card' }));
    const generateTokenButton = await tokensCard.getHarness(MatButtonHarness);
    const tokensTable = await tokensCard.getHarness(MatTableHarness);

    const cellTextByIndex = await tokensTable.getCellTextByIndex();
    expect(cellTextByIndex).toEqual([['No tokens']]);

    await generateTokenButton.click();

    const dialog = await rootLoader.getHarness(MatDialogHarness);
    expect(dialog).toBeTruthy();
  });

  it('should delete a token', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'ACTIVE',
    });
    const tokenResponse: Token = fakeUserToken({ name: 'My token', created_at: 1630373735403, last_use_at: 1631017105654 });

    expectUserTokensGetRequest(user, [tokenResponse]);
    expectUserGetRequest(user);
    expectEnvironmentListRequest();
    expectUserGroupsGetRequest(user.id, [
      fakeGroup({
        id: 'groupA',
        roles: { GROUP: 'ADMIN', API: 'ROLE_API', APPLICATION: 'ROLE_APP_OWNER', INTEGRATION: 'ROLE_INTEGRATION_OWNER' },
      }),
    ]);
    expectUserMembershipGetRequest(user.id, 'api');
    expectUserMembershipGetRequest(user.id, 'application');
    // expectUserMembershipGetRequest(user.id, 'integration');
    expectRolesListRequest('ORGANIZATION');
    expectRolesListRequest('API', [fakeRole({ id: 'roleApiId', name: 'ROLE_API' })]);
    expectRolesListRequest('APPLICATION', [
      fakeRole({ id: 'roleAppOwnerId', name: 'ROLE_APP_OWNER' }),
      fakeRole({ id: 'roleAppUserId', name: 'ROLE_APP_USER' }),
    ]);
    expectRolesListRequest('INTEGRATION', [fakeRole({ id: 'roleIntegrationId', name: 'ROLE_INTEGRATION_OWNER' })]);

    const tokensCard = await loader.getHarness(MatCardHarness.with({ selector: '.org-settings-user-detail__tokens__card' }));
    const tokensTable = await tokensCard.getHarness(MatTableHarness);
    expect(await tokensTable.getCellTextByIndex()).toEqual([
      ['My token', 'Aug 31, 2021, 1:35:35 AM', 'Sep 7, 2021, 12:18:25 PM', 'delete'],
    ]);

    const firstRowActionCell = (await (await tokensTable.getRows())[0].getCells({ columnName: 'action' }))[0];
    const removeTokenButton = await firstRowActionCell.getHarness(
      MatButtonHarness.with({ selector: '[aria-label="Button to delete a token"]' }),
    );

    await removeTokenButton.click();

    const dialog = await rootLoader.getHarness(MatDialogHarness);
    const removeButton = await dialog.getHarness(MatButtonHarness.with({ text: /Remove/ }));

    await removeButton.click();

    const reqDelete = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users/${user.id}/tokens/${tokenResponse.id}`);
    expect(reqDelete.request.method).toEqual('DELETE');
    reqDelete.flush(null);

    expectUserTokensGetRequest(user, []);
    expectUserGetRequest(user);
    expectEnvironmentListRequest();
    expectUserGroupsGetRequest(user.id, [
      fakeGroup({ id: 'groupA', roles: { GROUP: 'ADMIN', API: 'ROLE_API', APPLICATION: 'ROLE_APP_OWNER' } }),
    ]);
    expectUserMembershipGetRequest(user.id, 'api');
    expectUserMembershipGetRequest(user.id, 'application');
  });

  it('should have application name link', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'ACTIVE',
    });
    expectUserTokensGetRequest(user);
    expectUserGetRequest(user);
    expectEnvironmentListRequest();
    expectUserGroupsGetRequest(user.id);
    expectUserMembershipGetRequest(user.id, 'api');
    expectUserMembershipGetRequest(
      user.id,
      'application',
      fakeUserMembership('application', {
        metadata: {
          appFoxId: { name: 'Application Fox', environmentId: 'DEFAULT' },
          appDogId: { name: 'Application Dog', environmentId: 'DEFAULT' },
        },
      }),
    );
    expectRolesListRequest('ORGANIZATION');

    const clickableName = fixture.debugElement.query(By.css('a[href="/DEFAULT/applications/appFoxId"]'));
    expect(clickableName).toBeTruthy();
  });

  it('should display api name link', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'ACTIVE',
    });
    expectUserTokensGetRequest(user);
    expectUserGetRequest(user);
    expectEnvironmentListRequest();
    expectUserGroupsGetRequest(user.id);
    expectUserMembershipGetRequest(
      user.id,
      'api',
      fakeUserMembership('api', {
        metadata: {
          apiAlphaId: { name: 'API Alpha', version: '1.0.0', visibility: 'PUBLIC', environmentId: 'DEFAULT' },
          apiBetaId: { name: 'API Beta', version: '42.0.0', visibility: 'PRIVATE', environmentId: 'DEFAULT' },
        },
      }),
    );
    expectUserMembershipGetRequest(user.id, 'application');
    expectRolesListRequest('ORGANIZATION');

    const clickableName = fixture.debugElement.query(By.css('a[href="/DEFAULT/apis/apiAlphaId"]'));
    expect(clickableName).toBeTruthy();
  });

  function expectUserTokensGetRequest(user: User = fakeUser({ id: 'userId' }), tokens: Token[] = []) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users/${user.id}/tokens`);
    expect(req.request.method).toEqual('GET');
    req.flush(tokens);
    fixture.detectChanges();
  }

  function expectUserGetRequest(user: User = fakeUser({ id: 'userId' })) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users/${user.id}`);
    expect(req.request.method).toEqual('GET');
    req.flush(user);
    fixture.detectChanges();
  }

  function expectRolesListRequest(scope, roles: Role[] = []) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/${scope}/roles`);
    expect(req.request.method).toEqual('GET');
    req.flush(roles);
    fixture.detectChanges();
  }

  function expectUpdateUserRolesRequest(userId: string, referenceId: string, referenceType: string, roles: string[]) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users/${userId}/roles`);
    expect(req.request.method).toEqual('PUT');
    expect(req.request.body.referenceType).toEqual(referenceType);
    expect(req.request.body.referenceId).toEqual(referenceId);
    expect(req.request.body.roles).toEqual(roles);
    req.flush(null);
  }

  function expectEnvironmentListRequest(environments: Environment[] = []) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/environments`);
    expect(req.request.method).toEqual('GET');
    req.flush(environments);
    fixture.detectChanges();
  }

  function expectUserGroupsGetRequest(userId: string, groups: Group[] = []) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users/${userId}/groups`);
    expect(req.request.method).toEqual('GET');
    req.flush(groups);
    fixture.detectChanges();
  }

  function expectAddOrUpdateGroupMembershipRequest(groupId: string, groupMembership: GroupMembership[] = []) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/configuration/groups/${groupId}/members`);
    expect(req.request.method).toEqual('POST');
    req.flush(groupMembership);
    fixture.detectChanges();
  }

  function expectUserMembershipGetRequest<T extends 'api' | 'application'>(userId: string, type: T, userMembership?: UserMembership<T>) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users/${userId}/memberships?type=${type}`);
    expect(req.request.method).toEqual('GET');
    req.flush(userMembership ?? {});
  }

  function expectGroupListByOrganizationRequest(groups: Group[] = []) {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.org.baseURL}/groups`,
      })
      .flush(groups);
  }
});
