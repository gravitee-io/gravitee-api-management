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
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import {
  OrgSettingsUserDetailAddGroupDialogComponent,
  OrgSettingsUserDetailAddGroupDialogData,
} from './org-settings-user-detail-add-group-dialog.component';

import { Group } from '../../../../entities/group/group';
import { fakeGroup } from '../../../../entities/group/group.fixture';
import { GioTestingModule, CONSTANTS_TESTING } from '../../../../shared/testing';
import { OrganizationSettingsModule } from '../../organization-settings.module';
import { Role } from '../../../../entities/role/role';
import { fakeRole } from '../../../../entities/role/role.fixture';

describe('OrgSettingsUserDetailAddGroupDialogComponent', () => {
  let fixture: ComponentFixture<OrgSettingsUserDetailAddGroupDialogComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const matDialogRefMock = {
    close: jest.fn(),
  };

  afterEach(() => {
    matDialogRefMock.close.mockClear();
  });

  beforeEach(() => {
    const dialogData: OrgSettingsUserDetailAddGroupDialogData = {
      groupIdAlreadyAdded: [],
    };
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, OrganizationSettingsModule, GioTestingModule],
      providers: [
        {
          provide: MAT_DIALOG_DATA,
          useFactory: () => dialogData,
        },
        { provide: MatDialogRef, useValue: matDialogRefMock },
      ],
    });
    fixture = TestBed.createComponent(OrgSettingsUserDetailAddGroupDialogComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  it('should fill and submit form', async () => {
    fixture.detectChanges();
    expectGroupListByOrganizationRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
    fixture.detectChanges();
    expectRolesListRequest('API', [
      fakeRole({ id: 'roleOrgUserId', name: 'ROLE_API_USER' }),
      fakeRole({ id: 'roleOrgAdminId', name: 'ROLE_API_ADMIN' }),
    ]);
    expectRolesListRequest('APPLICATION', [
      fakeRole({ id: 'roleOrgUserId', name: 'ROLE_APPLICATION_USER' }),
      fakeRole({ id: 'roleOrgAdminId', name: 'ROLE_APPLICATION_ADMIN' }),
    ]);
    expectRolesListRequest('INTEGRATION', [
      fakeRole({ id: 'roleOrgUserId', name: 'ROLE_INTEGRATION_USER' }),
      fakeRole({ id: 'roleOrgAdminId', name: 'ROLE_INTEGRATION_ADMIN' }),
    ]);

    const submitButton = await loader.getHarness(MatButtonHarness.with({ selector: 'button[type=submit]' }));
    expect(await submitButton.isDisabled()).toBeTruthy();

    const groupIdSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName=groupId' }));
    await groupIdSelect.clickOptions({ text: 'Group A' });

    const apiRoleSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName=apiRole' }));
    await apiRoleSelect.clickOptions({ text: 'ROLE_API_USER' });

    const applicationRoleSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName=applicationRole' }));
    await applicationRoleSelect.clickOptions({ text: 'ROLE_APPLICATION_ADMIN' });

    const integrationRoleSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName=integrationRole' }));
    await integrationRoleSelect.clickOptions({ text: 'ROLE_INTEGRATION_ADMIN' });

    await submitButton.click();

    expect(matDialogRefMock.close).toHaveBeenCalledWith({
      apiRole: 'ROLE_API_USER',
      applicationRole: 'ROLE_APPLICATION_ADMIN',
      integrationRole: 'ROLE_INTEGRATION_ADMIN',
      groupId: 'group-a',
      isAdmin: null,
    });
  });

  function expectGroupListByOrganizationRequest(groups: Group[] = []) {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.org.baseURL}/groups`,
      })
      .flush(groups);
  }

  function expectRolesListRequest(scope, roles: Role[] = []) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/${scope}/roles`);
    expect(req.request.method).toEqual('GET');
    req.flush(roles);
    fixture.detectChanges();
  }
});
