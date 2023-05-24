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

import { ApiPortalGroupsMembersComponent } from './api-portal-groups-members.component';
import { ApiPortalGroupsMembersHarness } from './api-portal-groups-members.harness';

import { User } from '../../../../../../entities/user';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../../shared/testing';
import { ApiPortalUserGroupModule } from '../../api-portal-user-group.module';
import { CurrentUserService } from '../../../../../../services-ngx/current-user.service';
import { MembersResponse } from '../../../../../../entities/management-api-v2';
import { fakeMember } from '../../../../../../entities/management-api-v2/member/member.fixture';

@Component({
  selector: `host-component`,
  template: `<api-portal-groups-members [groupIds]="groupIds"></api-portal-groups-members>`,
})
class TestComponent {
  groupIds = ['groupId1', 'groupId2'];
}
describe('ApiPortalMembersComponent', () => {
  let fixture: ComponentFixture<TestComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;

  const currentUser = new User();

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, MatIconTestingModule, ApiPortalUserGroupModule],
      declarations: [ApiPortalGroupsMembersComponent, TestComponent],
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

  it('Display groups members tables', async () => {
    expectGroupsGetMembersRequest('groupId1', {
      data: [fakeMember({ roles: [{ name: 'USER', scope: 'API' }] })],
      metadata: { groupName: 'Group1' },
    });
    expectGroupsGetMembersRequest('groupId2', {
      data: [fakeMember({ roles: [{ name: 'USER', scope: 'APPLICATION' }] })],
      metadata: { groupName: 'Group2' },
    });
    fixture.detectChanges();

    const apiGroupsMembersComponent = await loader.getHarness(ApiPortalGroupsMembersHarness);

    const group1MembersTable = await apiGroupsMembersComponent.getGroupTableByGroupName('Group1');

    expect(await group1MembersTable.getCellTextByIndex()).toEqual([['', 'member-display-name', 'USER']]);

    const group2MembersTable = await apiGroupsMembersComponent.getGroupTableByGroupName('Group2');

    expect(await group2MembersTable.getCellTextByIndex()).toEqual([['', 'member-display-name', '']]);
  });

  function expectGroupsGetMembersRequest(groupId: string, membersResponse: MembersResponse) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups/${groupId}/members?page=1&perPage=9999`, method: 'GET' })
      .flush(membersResponse);
  }
});
