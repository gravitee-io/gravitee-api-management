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
import { InteractivityChecker } from '@angular/cdk/a11y';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { of } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { MatLegacyTableHarness as MatTableHarness } from '@angular/material/legacy-table/testing';
import { MatLegacyButtonHarness as MatButtonHarness } from '@angular/material/legacy-button/testing';
import { GioConfirmDialogHarness, GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { ActivatedRoute } from '@angular/router';

import { ApplicationGeneralMembersComponent } from './application-general-members.component';

import { ApplicationGeneralUserGroupModule } from '../application-general-user-group.module';
import { User } from '../../../../../entities/user';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { Role } from '../../../../../entities/role/role';
import { fakeRole } from '../../../../../entities/role/role.fixture';
import { RoleService } from '../../../../../services-ngx/role.service';
import { fakeGroup, fakeGroupsResponse, Member } from '../../../../../entities/management-api-v2';
import { fakeMembers } from '../../../../../entities/members/Members.fixture';
import { fakeApplication } from '../../../../../entities/application/Application.fixture';
import { Application } from '../../../../../entities/application/application';
import { fakeSearchableUser } from '../../../../../entities/user/searchableUser.fixture';
import { GioUsersSelectorHarness } from '../../../../../shared/components/gio-users-selector/gio-users-selector.harness';
import { GioTestingPermissionProvider } from '../../../../../shared/components/gio-permission/gio-permission.service';

describe('ApplicationGeneralMembersComponent', () => {
  let fixture: ComponentFixture<ApplicationGeneralMembersComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  const APPLICATION_ID = 'id_test';
  const currentUser = new User();

  const roles: Role[] = [
    fakeRole({ name: 'PRIMARY_OWNER', default: false }),
    fakeRole({ name: 'OWNER', default: false }),
    fakeRole({ name: 'USER', default: true }),
  ];

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, ApplicationGeneralUserGroupModule, GioHttpTestingModule, MatIconTestingModule],
      providers: [
        { provide: RoleService, useValue: { list: () => of(roles) } },
        { provide: ActivatedRoute, useValue: { currentUser, applicationId: APPLICATION_ID } },
        {
          provide: GioTestingPermissionProvider,
          useValue: ['application-member-u', 'application-member-d', 'application-member-c', 'application-member-r'],
        },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This checks focus trap, set it to true to  avoid the warning
          isTabbable: () => true,
        },
      })
      .overrideProvider(ActivatedRoute, { useValue: { snapshot: { params: { applicationId: APPLICATION_ID } } } });

    fixture = TestBed.createComponent(ApplicationGeneralMembersComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('List members', () => {
    it('should show all application members with roles', async () => {
      const applicationDetails = fakeApplication({ type: 'NATIVE' });
      const membersList = [fakeMembers()];

      expectRequests(applicationDetails, membersList);

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#membersTable' }));

      expect((await table.getRows()).length).toEqual(1);
    });
  });

  describe('Delete a member', () => {
    it('should delete application member except primary owner', async () => {
      const applicationDetails = fakeApplication({ type: 'NATIVE' });
      const membersList = [fakeMembers({ role: 'USER' })];

      expectRequests(applicationDetails, membersList);

      const button = await loader.getHarness(MatButtonHarness.with({ selector: `[aria-label="Delete member"]` }));

      await button.click();

      const confirmDialog = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
      expect(confirmDialog).toBeTruthy();
      expect(applicationDetails.id).toEqual('61840ad7-7a93-4b5b-840a-d77a937b5bff');
    });
  });

  describe('Add a member', () => {
    it('should add application member', async () => {
      const applicationDetails = fakeApplication({ type: 'NATIVE' });
      const membersList = [fakeMembers()];

      expectRequests(applicationDetails, membersList);

      const button = await loader.getHarness(MatButtonHarness.with({ selector: `[aria-label="Add member"]` }));
      await button.click();

      const usersSelector = await rootLoader.getHarness(GioUsersSelectorHarness);

      await usersSelector.typeSearch('testname');

      httpTestingController
        .expectOne(`${CONSTANTS_TESTING.org.baseURL}/search/users?q=testname`)
        .flush([fakeSearchableUser({ displayName: 'Testname', id: 'testname-id', reference: 'testname_ref' })]);
      await usersSelector.selectUser('Testname');

      await usersSelector.validate();

      const confirmDialog = await loader.getHarness(GioSaveBarHarness);
      await confirmDialog.clickSubmit();

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#membersTable' }));

      expect((await table.getRows()).length).toEqual(2);

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/members`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({ id: 'testname-id', role: 'USER' });
    });
  });

  function expectRequests(applicationDetails: Application, membersList: Member[]) {
    expectListApplicationRequest(applicationDetails);
    expectGetMembers(membersList);
    expectGetGroupsListRequest(applicationDetails.groups);
  }

  function expectGetMembers(members: Member[]) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/members`, method: 'GET' })
      .flush(members);
  }

  function expectListApplicationRequest(applicationDetails: Application) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}`,
        method: 'GET',
      })
      .flush(applicationDetails);
  }

  function expectGetGroupsListRequest(groups: string[]) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups?page=1&perPage=9999`, method: 'GET' })
      .flush(fakeGroupsResponse({ data: groups.map((id) => fakeGroup({ id, name: id + '-name' })) }));
    fixture.detectChanges();
  }
});
