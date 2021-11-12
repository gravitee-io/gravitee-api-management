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

import { OrgSettingsRoleMembersComponent } from './org-settings-role-members.component';

import { OrganizationSettingsModule } from '../organization-settings.module';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { MembershipListItem } from '../../../entities/role/membershipListItem';
import { fakeMembershipListItem } from '../../../entities/role/membershipListItem.fixture';
import { User } from '../../../entities/user';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { GioTableWrapperHarness } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.harness';

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

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, OrganizationSettingsModule],
      providers: [
        { provide: UIRouterState, useValue: {} },
        { provide: UIRouterStateParams, useValue: { roleScope, role } },
        { provide: CurrentUserService, useValue: { currentUser } },
      ],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(OrgSettingsRoleMembersComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
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
  }
});
