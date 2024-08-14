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
import { HarnessLoader } from '@angular/cdk/testing';
import { Component } from '@angular/core';

import { IntegrationGeneralGroupMembersComponent } from './integration-general-group-members.component';
import { IntegrationGeneralGroupMembersHarness } from './integration-general-group-members.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { IntegrationUserGroupModule } from '../../integration-user-group.module';
import { MembersResponse } from '../../../../../entities/management-api-v2';
import { fakeMember } from '../../../../../entities/management-api-v2/member/member.fixture';
import { GroupData } from '../integration-general-members.component';

const GROUP_ID = 'groupId1';
const GROUP_NAME = 'groupName1';

@Component({
  selector: `host-component`,
  template: `<integration-general-group-members [groupData]="groupData"></integration-general-group-members>`,
})
class TestComponent {
  groupData: GroupData = {
    id: GROUP_ID,
    name: GROUP_NAME,
  };
}
xdescribe('IntegrationGeneralGroupMembersComponent', () => {
  let fixture: ComponentFixture<TestComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, MatIconTestingModule, IntegrationUserGroupModule],
      declarations: [IntegrationGeneralGroupMembersComponent, TestComponent],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This checks focus trap, set it to true to  avoid the warning
        isTabbable: () => true,
      },
    });

    fixture = TestBed.createComponent(TestComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
  });

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  it('should display group members tables', async () => {
    expectGroupsGetMembersRequest({
      data: [fakeMember({ roles: [{ name: 'USER', scope: 'API' }] })],
      metadata: { groupName: GROUP_NAME },
      pagination: { totalCount: 1 },
    });

    fixture.detectChanges();

    const apiGroupsMembersComponent = await loader.getHarness(IntegrationGeneralGroupMembersHarness);

    const group1MembersTable = await apiGroupsMembersComponent.getGroupTableByGroupName();
    expect(await group1MembersTable.getCellTextByIndex()).toEqual([['', 'member-display-name', 'USER']]);
  });

  it('should group members tables -- no API role', async () => {
    expectGroupsGetMembersRequest({
      data: [fakeMember({ roles: [{ name: 'USER', scope: 'APPLICATION' }] })],
      metadata: { groupName: GROUP_NAME },
      pagination: { totalCount: 1 },
    });

    fixture.detectChanges();

    const apiGroupsMembersComponent = await loader.getHarness(IntegrationGeneralGroupMembersHarness);

    const group1MembersTable = await apiGroupsMembersComponent.getGroupTableByGroupName();
    expect(await group1MembersTable.getCellTextByIndex()).toEqual([['', 'member-display-name', '']]);
  });

  it('should not display group members tables if no members', async () => {
    expectGroupsGetMembersRequest({
      data: [],
      metadata: { groupName: GROUP_NAME },
      pagination: { totalCount: 0 },
    });

    fixture.detectChanges();

    const apiGroupsMembersComponent = await loader.getHarness(IntegrationGeneralGroupMembersHarness);
    expect(await apiGroupsMembersComponent.groupTableExistsByGroupName()).toEqual(false);
  });

  it('should display message if user lacks permissions', async () => {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups/groupId1/members?page=1&perPage=10`, method: 'GET' })
      .flush(
        { httpStatus: 403, message: 'You do not have the permissions to access this resource' },
        { statusText: 'Forbidden', status: 403 },
      );
    fixture.detectChanges();

    const apiGroupsMembersComponent = await loader.getHarness(IntegrationGeneralGroupMembersHarness);
    expect(await apiGroupsMembersComponent.userCannotViewGroupMembers()).toEqual(true);
  });

  function expectGroupsGetMembersRequest(membersResponse: MembersResponse) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups/${GROUP_ID}/members?page=1&perPage=10`, method: 'GET' })
      .flush(membersResponse);
  }
});
