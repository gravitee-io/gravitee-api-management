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
import { MatTabGroupHarness } from '@angular/material/tabs/testing';
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
import { GroupMembership } from '../../../../entities/group/groupMember';
import { fakeGroupMembership } from '../../../../entities/group/groupMember.fixture';
import { Token } from '../../../../entities/user/userTokens';
import { fakeUserToken } from '../../../../entities/user/userToken.fixture';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { User } from '../../../../entities/user/user';

describe('OrgSettingsUserDetailComponent', () => {
  let fixture: ComponentFixture<OrgSettingsUserDetailComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const defaultEnvironments = [
    fakeEnvironment({ id: 'envAlphaId', name: 'Environment Alpha' }),
    fakeEnvironment({ id: 'envBetaId', name: 'Environment Beta' }),
  ];

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
          isFocusable: () => true,
          isTabbable: () => true,
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
    expectInitRequests(user, defaultEnvironments, {}, [], {
      organization: [fakeRole({ id: 'roleOrgUserId', name: 'ROLE_ORG_USER' }), fakeRole({ id: 'roleOrgAdminId', name: 'ROLE_ORG_ADMIN' })],
    });

    const userCard = await loader.getHarness(MatCardHarness);

    const userCardText = await userCard.getText();
    expect(userCardText).toContain(user.displayName);
    expect(userCardText).toContain(user.email);
    expect(userCardText).toContain(customFields['custom-field-1']);
    expect(userCardText).toContain('ROLE_USER');

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
    expectInitRequests(user);

    const userCard = await loader.getHarness(MatCardHarness.with({ selector: '.org-settings-user-detail__card' }));
    const resetButton = await userCard.getHarness(MatButtonHarness.with({ text: 'Reset password' }));

    await resetButton.click();

    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (await dialog.getHarness(MatButtonHarness.with({ text: 'Reset' }))).click();

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users/${user.id}/resetPassword`);
    expect(req.request.method).toEqual('POST');
  });

  it('should not reset password if no firstname', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'management',
      email: null,
      firstname: null,
    });
    expectInitRequests(user);

    const userCard = await loader.getHarness(MatCardHarness.with({ selector: '.org-settings-user-detail__card' }));
    expect(await userCard.getAllHarnesses(MatButtonHarness.with({ text: 'Reset password' }))).toHaveLength(0);
  });

  it('should accept user registration after confirm dialog', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'PENDING',
    });
    expectInitRequests(user);

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
  });

  it('should reject user registration after confirm dialog', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'PENDING',
    });
    expectInitRequests(user);

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
  });

  it('should display registration banner', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'PENDING',
    });
    expectInitRequests(user);

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
    expectInitRequests(user);

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
    expectInitRequests(user, defaultEnvironments, {}, [], {
      organization: [fakeRole({ id: 'roleOrgUserId', name: 'ROLE_ORG_USER' }), fakeRole({ id: 'roleOrgAdminId', name: 'ROLE_ORG_ADMIN' })],
    });

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
      envRoles: { envAlphaId: [{ id: 'roleEnvApiId' }], envBetaId: [] },
    });
    expectInitRequests(user, defaultEnvironments, {}, [], {
      environment: [fakeRole({ id: 'roleEnvApiId', name: 'ROLE_ENV_API' }), fakeRole({ id: 'roleEnvUserId', name: 'ROLE_ENV_USER' })],
    });

    const environmentsCard = await loader.getHarness(MatCardHarness.with({ selector: '.org-settings-user-detail__environments-card' }));
    const environmentAlphaRolesSelect = await environmentsCard.getHarness(MatSelectHarness.with({ selector: '[id=envAlphaId]' }));

    await environmentAlphaRolesSelect.clickOptions({ text: 'ROLE_ENV_USER' });

    expect(await environmentAlphaRolesSelect.getValueText()).toBe('ROLE_ENV_API, ROLE_ENV_USER');

    const saveBar = await loader.getHarness(GioSaveBarHarness);
    await saveBar.clickSubmit();

    expectUpdateUserRolesRequest(user.id, 'envAlphaId', 'ENVIRONMENT', ['roleEnvApiId', 'roleEnvUserId']);
  });

  it('should display environment tabs in memberships card', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'ACTIVE',
    });
    expectInitRequests(user);

    const membershipsCard = await loader.getHarness(MatCardHarness.with({ selector: '.org-settings-user-detail__memberships-card' }));
    const tabGroup = await membershipsCard.getHarness(MatTabGroupHarness);
    const tabs = await tabGroup.getTabs();

    expect(tabs.length).toBe(2);
    expect(await tabs[0].getLabel()).toBe('Environment Alpha');
    expect(await tabs[1].getLabel()).toBe('Environment Beta');
  });

  it('should display APIs user membership in environment tab', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'ACTIVE',
    });
    expectInitRequests(user, defaultEnvironments, {
      envAlphaId: {
        apis: [
          {
            id: 'apiAlphaId',
            name: 'API Alpha',
            version: '1.0.0',
            visibility: 'PUBLIC',
            environmentId: 'envAlphaId',
            environmentName: 'Environment Alpha',
          },
          {
            id: 'apiBetaId',
            name: 'API Beta',
            version: '42.0.0',
            visibility: 'PRIVATE',
            environmentId: 'envAlphaId',
            environmentName: 'Environment Alpha',
          },
        ],
      },
    });

    const membershipsCard = await loader.getHarness(MatCardHarness.with({ selector: '.org-settings-user-detail__memberships-card' }));
    const apisTable = await membershipsCard.getHarness(MatTableHarness.with({ selector: '[aria-label="APIs table"]' }));

    expect(await apisTable.getCellTextByIndex()).toEqual([
      ['API Alpha', '1.0.0', 'public Public'],
      ['API Beta', '42.0.0', 'lock Private'],
    ]);
  });

  it('should display applications user membership in environment tab', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'ACTIVE',
    });
    expectInitRequests(user, defaultEnvironments, {
      envAlphaId: {
        applications: [
          { id: 'appFoxId', name: 'Application Fox', environmentId: 'envAlphaId', environmentName: 'Environment Alpha' },
          { id: 'appDogId', name: 'Application Dog', environmentId: 'envAlphaId', environmentName: 'Environment Alpha' },
        ],
      },
    });

    const membershipsCard = await loader.getHarness(MatCardHarness.with({ selector: '.org-settings-user-detail__memberships-card' }));
    const applicationsTable = await membershipsCard.getHarness(MatTableHarness.with({ selector: '[aria-label="Applications table"]' }));

    expect(await applicationsTable.getCellTextByIndex()).toEqual([['Application Fox'], ['Application Dog']]);
  });

  it('should load memberships when switching tab', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'ACTIVE',
    });
    expectInitRequests(user, defaultEnvironments, {
      envAlphaId: {
        apis: [
          {
            id: 'apiAlphaId',
            name: 'API Alpha',
            version: '1.0.0',
            visibility: 'PUBLIC',
            environmentId: 'envAlphaId',
            environmentName: 'Environment Alpha',
          },
        ],
      },
    });

    const membershipsCard = await loader.getHarness(MatCardHarness.with({ selector: '.org-settings-user-detail__memberships-card' }));
    const tabGroup = await membershipsCard.getHarness(MatTabGroupHarness);
    const tabs = await tabGroup.getTabs();

    // Switch to second tab
    await tabs[1].select();

    // Expect v2 calls for second environment
    expectUserGroupsV2Request('userId', 'envBetaId', []);
    expectUserApisV2Request('userId', 'envBetaId', [
      {
        id: 'apiBetaId',
        name: 'API Beta',
        version: '2.0.0',
        visibility: 'PRIVATE',
        environmentId: 'envBetaId',
        environmentName: 'Environment Beta',
      },
    ]);
    expectUserApplicationsV2Request('userId', 'envBetaId', []);

    fixture.detectChanges();

    const apisTable = await membershipsCard.getHarness(MatTableHarness.with({ selector: '[aria-label="APIs table"]' }));
    expect(await apisTable.getCellTextByIndex()).toEqual([['API Beta', '2.0.0', 'lock Private']]);
  });

  it('should save group roles', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'ACTIVE',
    });
    expectInitRequests(
      user,
      defaultEnvironments,
      {
        envAlphaId: {
          groups: [
            {
              id: 'groupA',
              name: 'Group A',
              environmentId: 'envAlphaId',
              environmentName: 'Environment Alpha',
              roles: { GROUP: 'ADMIN', API: 'ROLE_API', APPLICATION: 'ROLE_APP_OWNER', INTEGRATION: 'ROLE_INTEGRATION_OWNER' },
            },
          ],
        },
      },
      [],
      {
        api: [fakeRole({ id: 'roleApiId', name: 'ROLE_API' })],
        application: [fakeRole({ id: 'roleAppOwnerId', name: 'ROLE_APP_OWNER' }), fakeRole({ id: 'roleAppUserId', name: 'ROLE_APP_USER' })],
        integration: [fakeRole({ id: 'roleIntegrationOwnerId', name: 'ROLE_INTEGRATION_OWNER' })],
      },
    );

    const membershipsCard = await loader.getHarness(MatCardHarness.with({ selector: '.org-settings-user-detail__memberships-card' }));
    const groupsTable = await membershipsCard.getHarness(MatTableHarness.with({ selector: '[aria-label="Groups table"]' }));

    const groupAdminCheckbox = await (await (await groupsTable.getRows())[0].getCells())[1].getHarness(MatCheckboxHarness);
    const apiRoleSelect = await (await (await groupsTable.getRows())[0].getCells())[2].getHarness(MatSelectHarness);
    const applicationRoleSelect = await (await (await groupsTable.getRows())[0].getCells())[3].getHarness(MatSelectHarness);
    const integrationRoleSelect = await (await (await groupsTable.getRows())[0].getCells())[4].getHarness(MatSelectHarness);

    expect(await groupAdminCheckbox.isChecked()).toBe(true);
    expect(await apiRoleSelect.getValueText()).toBe('ROLE_API');
    expect(await applicationRoleSelect.getValueText()).toBe('ROLE_APP_OWNER');
    expect(await integrationRoleSelect.getValueText()).toBe('ROLE_INTEGRATION_OWNER');

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
      source: 'ACTIVE',
    });
    expectInitRequests(user, defaultEnvironments, {
      envAlphaId: {
        groups: [
          { id: 'groupA', name: 'Group A', environmentId: 'envAlphaId', environmentName: 'Environment Alpha', roles: { GROUP: 'ADMIN' } },
        ],
      },
    });

    const membershipsCard = await loader.getHarness(MatCardHarness.with({ selector: '.org-settings-user-detail__memberships-card' }));
    const groupsTable = await membershipsCard.getHarness(MatTableHarness.with({ selector: '[aria-label="Groups table"]' }));

    const deleteUserGroupButton = await (await (await groupsTable.getRows())[0].getCells())[5].getHarness(MatButtonHarness);

    await deleteUserGroupButton.click();

    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (await dialog.getHarness(MatButtonHarness.with({ text: 'Delete' }))).click();

    const req = httpTestingController.expectOne(
      `${CONSTANTS_TESTING.org.baseURL}/environments/envAlphaId/configuration/groups/groupA/members/${user.id}`,
    );
    expect(req.request.method).toEqual('DELETE');
  });

  it('should add and save group roles', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'ACTIVE',
    });
    expectInitRequests(user, defaultEnvironments, {
      envAlphaId: {
        groups: [{ id: 'groupA', name: 'Group A', environmentId: 'envAlphaId', environmentName: 'Environment Alpha', roles: {} }],
      },
    });

    const membershipsCard = await loader.getHarness(MatCardHarness.with({ selector: '.org-settings-user-detail__memberships-card' }));

    const addGroupButton = await membershipsCard.getHarness(MatButtonHarness.with({ text: /Add a group/ }));
    await addGroupButton.click();
    fixture.detectChanges();
    expectGroupsV2ByEnvironmentRequest('envAlphaId', [
      { id: 'groupA', name: 'Group A' },
      { id: 'groupB', name: 'Group B' },
    ]);

    const dialog = await rootLoader.getHarness(MatDialogHarness);
    expectRolesListRequest('API');
    expectRolesListRequest('APPLICATION');
    expectRolesListRequest('INTEGRATION');

    const groupIdSelect = await dialog.getHarness(MatSelectHarness.with({ selector: '[formControlName="groupId"]' }));
    await groupIdSelect.open();
    // group A option is filtered (already added)
    expect((await groupIdSelect.getOptions()).length).toEqual(1);

    await groupIdSelect.clickOptions({ text: 'Group B' });

    const isAdminSelect = await dialog.getHarness(MatCheckboxHarness.with({ selector: '[formControlName="isAdmin"]' }));
    await isAdminSelect.check();

    const submitButton = await dialog.getHarness(MatButtonHarness.with({ selector: 'button[type=submit]' }));
    await submitButton.click();

    const req = httpTestingController.expectOne(
      `${CONSTANTS_TESTING.org.baseURL}/environments/envAlphaId/configuration/groups/groupB/members`,
    );
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
    req.flush([fakeGroupMembership({ id: 'userId', roles: [{ scope: 'GROUP', name: 'ADMIN' }] })]);

    fixture.detectChanges();

    // requestReload triggers ngOnInit + memberships reload (roles are cached via shareReplay)
    expectUserTokensGetRequest(user);
    expectUserGetRequest(user);
    expectEnvironmentListRequest(defaultEnvironments);
    expectUserGroupsV2Request(user.id, 'envAlphaId', [
      { id: 'groupA', name: 'Group A', environmentId: 'envAlphaId', environmentName: 'Environment Alpha', roles: {} },
      { id: 'groupB', name: 'Group B', environmentId: 'envAlphaId', environmentName: 'Environment Alpha', roles: { GROUP: 'ADMIN' } },
    ]);
    expectUserApisV2Request(user.id, 'envAlphaId');
    expectUserApplicationsV2Request(user.id, 'envAlphaId');

    const membershipsCardAfterAdd = await loader.getHarness(
      MatCardHarness.with({ selector: '.org-settings-user-detail__memberships-card' }),
    );
    const groupsTableAfterAdd = await membershipsCardAfterAdd.getHarness(MatTableHarness.with({ selector: '[aria-label="Groups table"]' }));
    const rows = await groupsTableAfterAdd.getRows();
    expect(rows.length).toBe(2);
  });

  it('should open dialog to create a token', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'ACTIVE',
    });
    const groupData = {
      id: 'groupA',
      name: 'Group A',
      environmentId: 'envAlphaId',
      environmentName: 'Environment Alpha',
      roles: { GROUP: 'ADMIN', API: 'ROLE_API', APPLICATION: 'ROLE_APP_OWNER', INTEGRATION: 'ROLE_INTEGRATION_OWNER' },
    };
    expectInitRequests(user, defaultEnvironments, { envAlphaId: { groups: [groupData] } }, [], {
      api: [fakeRole({ id: 'roleApiId', name: 'ROLE_API' })],
      application: [fakeRole({ id: 'roleAppOwnerId', name: 'ROLE_APP_OWNER' }), fakeRole({ id: 'roleAppUserId', name: 'ROLE_APP_USER' })],
      integration: [fakeRole({ id: 'roleApiId', name: 'ROLE_INTEGRATION_OWNER' })],
    });

    const tokensCard = await loader.getHarness(MatCardHarness.with({ selector: '.org-settings-user-detail__tokens__card' }));
    const generateTokenButton = await tokensCard.getHarness(MatButtonHarness);
    const tokensTable = await tokensCard.getHarness(MatTableHarness);

    const tokensTableHost = await tokensTable.host();
    expect(await tokensTableHost.text()).toContain('No tokens');

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
    const groupData = {
      id: 'groupA',
      name: 'Group A',
      environmentId: 'envAlphaId',
      environmentName: 'Environment Alpha',
      roles: { GROUP: 'ADMIN', API: 'ROLE_API', APPLICATION: 'ROLE_APP_OWNER', INTEGRATION: 'ROLE_INTEGRATION_OWNER' },
    };
    expectInitRequests(user, defaultEnvironments, { envAlphaId: { groups: [groupData] } }, [tokenResponse], {
      api: [fakeRole({ id: 'roleApiId', name: 'ROLE_API' })],
      application: [fakeRole({ id: 'roleAppOwnerId', name: 'ROLE_APP_OWNER' }), fakeRole({ id: 'roleAppUserId', name: 'ROLE_APP_USER' })],
      integration: [fakeRole({ id: 'roleIntegrationId', name: 'ROLE_INTEGRATION_OWNER' })],
    });

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

    // After ngOnInit re-runs
    expectApiSearchRequest();
    expectUserTokensGetRequest(user, []);
    expectUserGetRequest(user);
    expectEnvironmentListRequest();
  });

  it('should have application name link', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'ACTIVE',
    });
    expectInitRequests(user, defaultEnvironments, {
      envAlphaId: {
        applications: [
          { id: 'appFoxId', name: 'Application Fox', environmentId: 'envAlphaId', environmentName: 'Environment Alpha' },
          { id: 'appDogId', name: 'Application Dog', environmentId: 'envAlphaId', environmentName: 'Environment Alpha' },
        ],
      },
    });

    const clickableName = fixture.debugElement.query(By.css('a[href="/envAlphaId/applications/appFoxId"]'));
    expect(clickableName).toBeTruthy();
  });

  it('should display api name link', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'ACTIVE',
    });
    expectInitRequests(user, defaultEnvironments, {
      envAlphaId: {
        apis: [
          {
            id: 'apiAlphaId',
            name: 'API Alpha',
            version: '1.0.0',
            visibility: 'PUBLIC',
            environmentId: 'envAlphaId',
            environmentName: 'Environment Alpha',
          },
        ],
      },
    });

    const clickableName = fixture.debugElement.query(By.css('a[href="/envAlphaId/apis/apiAlphaId"]'));
    expect(clickableName).toBeTruthy();
  });

  it('should show empty state in tab with no memberships', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'ACTIVE',
    });
    expectInitRequests(user);

    const membershipsCard = await loader.getHarness(MatCardHarness.with({ selector: '.org-settings-user-detail__memberships-card' }));

    const apisTable = await membershipsCard.getHarness(MatTableHarness.with({ selector: '[aria-label="APIs table"]' }));
    expect(await apisTable.getCellTextByIndex()).toEqual([['No API']]);

    const applicationsTable = await membershipsCard.getHarness(MatTableHarness.with({ selector: '[aria-label="Applications table"]' }));
    expect(await applicationsTable.getCellTextByIndex()).toEqual([['No application']]);

    const groupsTable = await membershipsCard.getHarness(MatTableHarness.with({ selector: '[aria-label="Groups table"]' }));
    expect(await groupsTable.getCellTextByIndex()).toEqual([['No group']]);
  });

  // ---- Helper functions ----

  function expectInitRequests(
    user: User = fakeUser({ id: 'userId' }),
    environments: Environment[] = defaultEnvironments,
    v2MembershipsPerEnv: Record<string, { groups?: any[]; apis?: any[]; applications?: any[] }> = {},
    tokens: Token[] = [],
    roles: {
      organization?: Role[];
      environment?: Role[];
      api?: Role[];
      application?: Role[];
      integration?: Role[];
    } = {},
  ) {
    expectApiSearchRequest();
    expectUserTokensGetRequest(user, tokens);
    expectUserGetRequest(user);
    expectEnvironmentListRequest(environments);

    // After environments load, the memberships sub-component auto-selects first tab
    if (environments.length > 0) {
      const firstEnvId = environments[0].id;
      const firstEnvData = v2MembershipsPerEnv[firstEnvId] || {};
      expectUserGroupsV2Request(user.id, firstEnvId, firstEnvData.groups || []);
      expectUserApisV2Request(user.id, firstEnvId, firstEnvData.apis || []);
      expectUserApplicationsV2Request(user.id, firstEnvId, firstEnvData.applications || []);
    }

    // Flush role requests always fired by toSignal() in memberships component and parent template
    expectRolesListRequest('ORGANIZATION', roles.organization || []);
    expectRolesListRequest('API', roles.api || []);
    expectRolesListRequest('APPLICATION', roles.application || []);
    expectRolesListRequest('INTEGRATION', roles.integration || []);
    expectRolesListRequest('ENVIRONMENT', roles.environment || []);
  }

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

  function expectEnvironmentListRequest(environments: Environment[] = defaultEnvironments) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/environments`);
    expect(req.request.method).toEqual('GET');
    req.flush(environments);
    fixture.detectChanges();
  }

  function expectAddOrUpdateGroupMembershipRequest(groupId: string, groupMembership: GroupMembership[] = []) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/configuration/groups/${groupId}/members`);
    expect(req.request.method).toEqual('POST');
    req.flush(groupMembership);
    fixture.detectChanges();
  }

  function expectGroupsV2ByEnvironmentRequest(environmentId: string, groups: any[] = []) {
    const req = httpTestingController.expectOne(
      (request) => request.method === 'GET' && request.url === `${CONSTANTS_TESTING.v2BaseURL}/environments/${environmentId}/groups`,
    );
    req.flush({
      data: groups,
      pagination: {
        page: 1,
        perPage: 9999,
        totalCount: groups.length,
        pageCount: groups.length > 0 ? 1 : 0,
        pageItemsCount: groups.length,
      },
    });
    fixture.detectChanges();
  }

  function expectApiSearchRequest() {
    const req = httpTestingController.expectOne(
      request => request.method === 'POST' && request.url.includes('/management/v2/environments/') && request.url.includes('/apis/_search'),
    );
    expect(req.request.method).toEqual('POST');
    req.flush({ data: [], pagination: { page: 1, perPage: 10000, totalCount: 0, pageCount: 0, pageItemsCount: 0 } });
    fixture.detectChanges();
  }

  function expectUserApisV2Request(userId: string, environmentId: string, apis: any[] = []) {
    const req = httpTestingController.expectOne(
      (request) =>
        request.method === 'GET' &&
        request.url === `${CONSTANTS_TESTING.org.v2BaseURL}/users/${userId}/apis` &&
        request.params.get('environmentId') === environmentId,
    );
    req.flush({
      data: apis,
      pagination: { page: 1, perPage: 9999, totalCount: apis.length, pageCount: apis.length > 0 ? 1 : 0, pageItemsCount: apis.length },
    });
    fixture.detectChanges();
  }

  function expectUserGroupsV2Request(userId: string, environmentId: string, groups: any[] = []) {
    const req = httpTestingController.expectOne(
      (request) =>
        request.method === 'GET' &&
        request.url === `${CONSTANTS_TESTING.org.v2BaseURL}/users/${userId}/groups` &&
        request.params.get('environmentId') === environmentId,
    );
    req.flush({
      data: groups,
      pagination: {
        page: 1,
        perPage: 9999,
        totalCount: groups.length,
        pageCount: groups.length > 0 ? 1 : 0,
        pageItemsCount: groups.length,
      },
    });
    fixture.detectChanges();
  }

  function expectUserApplicationsV2Request(userId: string, environmentId: string, applications: any[] = []) {
    const req = httpTestingController.expectOne(
      (request) =>
        request.method === 'GET' &&
        request.url === `${CONSTANTS_TESTING.org.v2BaseURL}/users/${userId}/applications` &&
        request.params.get('environmentId') === environmentId,
    );
    req.flush({
      data: applications,
      pagination: {
        page: 1,
        perPage: 9999,
        totalCount: applications.length,
        pageCount: applications.length > 0 ? 1 : 0,
        pageItemsCount: applications.length,
      },
    });
    fixture.detectChanges();
  }
});
