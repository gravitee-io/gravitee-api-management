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

import { ApiPortalGroupsMembersComponent } from './api-portal-groups-members.component';

import { User } from '../../../../../../entities/user';
import { GioHttpTestingModule } from '../../../../../../shared/testing';
import { ApiPortalUserGroupModule } from '../../api-portal-user-group.module';
import { UIRouterStateParams } from '../../../../../../ajs-upgraded-providers';
import { CurrentUserService } from '../../../../../../services-ngx/current-user.service';

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

  it('should work', async () => {
    expect(loader).toBeTruthy();
  });
});
