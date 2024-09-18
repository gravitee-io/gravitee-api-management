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

import { ApiGeneralGroupMembersComponent } from './api-general-group-members.component';
import { ApiGeneralGroupMembersHarness } from './api-general-group-members.harness';

import { User } from '../../../../../../entities/user';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../../shared/testing';
import { ApiGeneralUserGroupModule } from '../../api-general-user-group.module';
import { CurrentUserService } from '../../../../../../services-ngx/current-user.service';
import { MembersResponse } from '../../../../../../entities/management-api-v2';
import { fakeMember } from '../../../../../../entities/management-api-v2/member/member.fixture';
import { GroupData } from '../api-general-members.component';

const GROUP_ID = 'groupId1';
const GROUP_NAME = 'groupName1';

@Component({
  selector: `host-component`,
  template: `<api-general-group-members [groupData]="groupData" (destroy)="isDestroy()"></api-general-group-members>`,
})
class TestComponent {
  groupData: GroupData = {
    id: GROUP_ID,
    name: GROUP_NAME,
  };
  destroy = false;

  isDestroy() {
    this.destroy = true;
  }
}
describe('ApiGeneralGroupMembersComponent', () => {
  let fixture: ComponentFixture<TestComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;

  const currentUser = new User();

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, MatIconTestingModule, ApiGeneralUserGroupModule],
      declarations: [ApiGeneralGroupMembersComponent, TestComponent],
      providers: [{ provide: CurrentUserService, useValue: { currentUser } }],
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
    const apiGroupsMembersComponent = await loader.getHarness(ApiGeneralGroupMembersHarness);

    expect(await apiGroupsMembersComponent.isLoading()).toEqual(true);

    expectGroupsGetMembersRequest({
      data: [fakeMember({ roles: [{ name: 'USER', scope: 'API' }] })],
      metadata: { groupName: GROUP_NAME },
      pagination: { totalCount: 1 },
    });

    fixture.detectChanges();

    expect(await apiGroupsMembersComponent.isLoading()).toEqual(false);
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

    const apiGroupsMembersComponent = await loader.getHarness(ApiGeneralGroupMembersHarness);

    const group1MembersTable = await apiGroupsMembersComponent.getGroupTableByGroupName();
    expect(await group1MembersTable.getCellTextByIndex()).toEqual([['', 'member-display-name', '']]);
  });

  it('should not display group members tables if no members', async () => {
    const apiGroupsMembersComponent = await loader.getHarness(ApiGeneralGroupMembersHarness);
    expect(fixture.componentInstance.destroy).toEqual(false);

    expectGroupsGetMembersRequest({
      data: [],
      metadata: { groupName: GROUP_NAME },
      pagination: { totalCount: 0 },
    });

    fixture.detectChanges();
    expect(fixture.componentInstance.destroy).toEqual(true);
    expect(await apiGroupsMembersComponent.isLoading()).toEqual(true);
  });

  it('should display message if user lacks permissions', async () => {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups/groupId1/members?page=1&perPage=10`, method: 'GET' })
      .flush(
        { httpStatus: 403, message: 'You do not have the permissions to access this resource' },
        { statusText: 'Forbidden', status: 403 },
      );
    fixture.detectChanges();

    const apiGroupsMembersComponent = await loader.getHarness(ApiGeneralGroupMembersHarness);
    expect(await apiGroupsMembersComponent.userCannotViewGroupMembers()).toEqual(true);
  });

  function expectGroupsGetMembersRequest(membersResponse: MembersResponse) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups/${GROUP_ID}/members?page=1&perPage=10`, method: 'GET' })
      .flush(membersResponse);
  }
});
