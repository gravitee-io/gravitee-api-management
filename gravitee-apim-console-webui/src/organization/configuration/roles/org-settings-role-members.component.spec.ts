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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatCellHarness } from '@angular/material/table/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { OrgSettingsRoleMembersComponent } from './org-settings-role-members.component';

import { OrganizationSettingsModule } from '../organization-settings.module';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { MembershipListItem } from '../../../entities/role/membershipListItem';
import { fakeMembershipListItem } from '../../../entities/role/membershipListItem.fixture';
import { User } from '../../../entities/user';
import { GioTableWrapperHarness } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.harness';
import { GioUsersSelectorHarness } from '../../../shared/components/gio-users-selector/gio-users-selector.harness';
import { fakeSearchableUser } from '../../../entities/user/searchableUser.fixture';

describe('OrgSettingsRoleMembersComponent', () => {
  const roleScope = 'ORGANIZATION';
  const role = 'USER';

  const currentUser = new User();
  currentUser.userPermissions = [];
  currentUser.userApiPermissions = [];
  currentUser.userEnvironmentPermissions = [];
  currentUser.userApplicationPermissions = [];

  let fixture: ComponentFixture<OrgSettingsRoleMembersComponent>;
  let component: OrgSettingsRoleMembersComponent;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, OrganizationSettingsModule],
      providers: [
        { provide: UIRouterState, useValue: {} },
        { provide: UIRouterStateParams, useValue: { roleScope, role } },
        { provide: CurrentUserService, useValue: { currentUser } },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
      },
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(OrgSettingsRoleMembersComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should init table with data', async () => {
    expect(component.roleScope).toEqual(roleScope);
    expect(component.role).toEqual(role);

    respondToListMembershipsRequest([
      fakeMembershipListItem({
        displayName: 'John Doe',
      }),
      fakeMembershipListItem({
        displayName: 'Gaëtan',
      }),
    ]);

    await loader.getAllHarnesses(MatCellHarness.with({ columnName: 'displayName', text: 'John Doe' }));
    await loader.getAllHarnesses(MatCellHarness.with({ columnName: 'displayName', text: 'Gaëtan' }));

    const searchInput = await loader.getHarness(GioTableWrapperHarness);
    await searchInput.setSearchValue('John');

    const tableCells = await loader.getAllHarnesses(MatCellHarness.with({ columnName: 'displayName' }));
    expect(tableCells.length).toEqual(1);
    expect(await tableCells[0].getText()).toEqual('John Doe');
  });

  describe('onDeleteMemberClicked', () => {
    beforeEach(() => {
      currentUser.userPermissions = ['organization-role-u'];
    });

    it('should delete member', async () => {
      respondToListMembershipsRequest([
        fakeMembershipListItem({
          id: 'user#1',
          displayName: 'John Doe',
        }),
      ]);

      const deleteButton = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Button to delete a member"]' }));
      await deleteButton.click();

      const confirmDialogButton = await rootLoader.getHarness(MatButtonHarness.with({ text: 'Delete' }));
      await confirmDialogButton.click();

      httpTestingController
        .expectOne({
          method: 'DELETE',
          url: `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/${roleScope}/roles/${role}/users/user#1`,
        })
        .flush(null);

      respondToListMembershipsRequest([]);
    });
  });

  describe('onAddMemberClicked', () => {
    beforeEach(() => {
      currentUser.userPermissions = ['organization-role-u'];
    });

    it('should add members', async () => {
      respondToListMembershipsRequest([
        fakeMembershipListItem({
          id: 'user#1',
          displayName: 'John Doe',
        }),
      ]);

      const addButton = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Button to add a member"]' }));
      await addButton.click();

      const usersSelector = await rootLoader.getHarness(GioUsersSelectorHarness);

      await usersSelector.typeSearch('john');
      httpTestingController
        .expectOne(`${CONSTANTS_TESTING.org.baseURL}/search/users?q=john`)
        .flush([fakeSearchableUser({ displayName: 'John', id: 'john', reference: 'john_ref' })]);
      await usersSelector.selectUser('John');

      await usersSelector.typeSearch('flash');
      httpTestingController
        .expectOne(`${CONSTANTS_TESTING.org.baseURL}/search/users?q=flash`)
        .flush([fakeSearchableUser({ displayName: 'Flash', id: 'flash', reference: 'flash_ref' })]);
      await usersSelector.selectUser('Flash');

      await usersSelector.validate();

      const membershipRequests = httpTestingController.match({
        method: 'POST',
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/${roleScope}/roles/${role}/users`,
      });

      expect(membershipRequests.length).toEqual(2);

      expect(membershipRequests[1].request.body).toStrictEqual({
        id: 'john',
        reference: 'john_ref',
      });
      membershipRequests[1].flush(null);

      expect(membershipRequests[0].request.body).toStrictEqual({
        id: 'flash',
        reference: 'flash_ref',
      });
      membershipRequests[0].flush(null);

      respondToListMembershipsRequest([]);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  function respondToListMembershipsRequest(items: MembershipListItem[]) {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/${roleScope}/roles/${role}/users`,
      })
      .flush(items);

    fixture.detectChanges();
  }
});
