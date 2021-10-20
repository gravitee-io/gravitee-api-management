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
import { MatChipHarness } from '@angular/material/chips/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

import { OrgSettingsUserDetailComponent } from './org-settings-user-detail.component';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { OrganizationSettingsModule } from '../../organization-settings.module';
import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { User } from '../../../../entities/user/user';
import { fakeUser } from '../../../../entities/user/user.fixture';
import { User as DeprecatedUser } from '../../../../entities/user';
import { Role } from '../../../../entities/role/role';
import { fakeRole } from '../../../../entities/role/role.fixture';
import { GioSaveBarHarness } from '../../../../shared/components/gio-save-bar/gio-save-bar.harness';

describe('OrgSettingsUserDetailComponent', () => {
  const fakeAjsState = {
    go: jest.fn(),
  };

  let fixture: ComponentFixture<OrgSettingsUserDetailComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    const currentUser = new DeprecatedUser();
    currentUser.userPermissions = ['organization-user-u'];

    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, OrganizationSettingsModule],
      providers: [
        { provide: UIRouterState, useValue: fakeAjsState },
        { provide: UIRouterStateParams, useValue: { userId: 'userId' } },
        { provide: CurrentUserService, useValue: { currentUser } },
      ],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(OrgSettingsUserDetailComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

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
    expectUserGetRequest(user);
    expectRolesListRequest('ORGANIZATION', [
      fakeRole({ id: 'roleOrgUserId', name: 'ROLE_ORG_USER' }),
      fakeRole({ id: 'roleOrgAdminId', name: 'ROLE_ORG_ADMIN' }),
    ]);

    const userCard = await loader.getHarness(MatCardHarness);

    [user.displayName, user.email, customFields['custom-field-1'], 'ROLE_USER'].forEach(async (value) => {
      expect(await userCard.getText()).toContain(value);
    });

    const userStatus = await userCard.getHarness(MatChipHarness.with({ ancestor: '.org-settings-user-detail__card__head__middle__tags' }));
    expect(await userStatus.getText()).toContain(user.status);

    const resetPasswordButton = await userCard.getHarness(MatButtonHarness.with({ text: 'Reset password' }));
    expect(resetPasswordButton).toBeTruthy();
  });

  it('should display registration banner', async () => {
    const user = fakeUser({
      id: 'userId',
      source: 'gravitee',
      status: 'PENDING',
    });
    expectUserGetRequest(user);
    expectRolesListRequest('ORGANIZATION', []);

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
    expectUserGetRequest(user);
    expectRolesListRequest('ORGANIZATION', []);

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
    expectUserGetRequest(user);
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

    expectUpdateUserRolesRequest(user.id, ['roleOrgUserId', 'roleOrgAdminId']);
  });

  function expectUserGetRequest(user: User = fakeUser({ id: 'userId' })) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users/${user.id}`);
    expect(req.request.method).toEqual('GET');
    req.flush(user);
    fixture.detectChanges();
  }

  function expectRolesListRequest(scope, roles: Role[]) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/${scope}/roles`);
    expect(req.request.method).toEqual('GET');
    req.flush(roles);
    fixture.detectChanges();
  }

  function expectUpdateUserRolesRequest(userId: string, roles: string[]) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users/${userId}/roles`);
    expect(req.request.method).toEqual('PUT');
    expect(req.request.body.roles).toEqual(roles);
    req.flush({});
  }
});
