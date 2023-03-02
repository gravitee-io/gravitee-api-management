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
import { MatTableHarness } from '@angular/material/table/testing';

import { ApiPortalGroupsMembersComponent } from './api-portal-groups-members.component';

import { User } from '../../../../../../entities/user';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../../shared/testing';
import { ApiPortalUserGroupModule } from '../../api-portal-user-group.module';
import { UIRouterStateParams } from '../../../../../../ajs-upgraded-providers';
import { CurrentUserService } from '../../../../../../services-ngx/current-user.service';
import { Group } from '../../../../../../entities/group/group';
import { GroupMember } from '../../../../../../entities/group/groupMember';
import { fakeGroupMember } from '../../../../../../entities/group/groupMember.fixture';
import { fakeGroup } from '../../../../../../entities/group/group.fixture';

describe('ApiPortalMembersComponent', () => {
  let fixture: ComponentFixture<ApiPortalGroupsMembersComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;

  const currentUser = new User();
  const apiId = 'apiId';

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, MatIconTestingModule, ApiPortalUserGroupModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId } },
        { provide: CurrentUserService, useValue: { currentUser } },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This checks focus trap, set it to true to  avoid the warning
        isTabbable: () => true,
      },
    });

    fixture = TestBed.createComponent(ApiPortalGroupsMembersComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
  });

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  it('Display groups members tables', async () => {
    expectGroupIdsWithMembersGetRequest({
      groupId1: [fakeGroupMember()],
    });
    expectGroupsGetRequest([fakeGroup({ id: 'groupId1', name: 'Group1' })]);

    fixture.detectChanges();

    const groupsMembersTable = await loader.getHarness(MatTableHarness.with({ selector: '[aria-label="Group Group1 members table"]' }));

    expect(await groupsMembersTable.getCellTextByIndex()).toEqual([['', 'Joe Bar', 'USER']]);
  });

  function expectGroupIdsWithMembersGetRequest(groupIdsMembers: Record<string, GroupMember[]>) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/groups`, method: 'GET' }).flush(groupIdsMembers);
  }

  function expectGroupsGetRequest(groups: Group[] = []) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/configuration/groups`, method: 'GET' }).flush(groups);
  }
});
