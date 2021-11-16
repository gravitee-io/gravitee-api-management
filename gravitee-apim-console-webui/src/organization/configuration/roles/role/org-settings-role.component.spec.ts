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
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatInputHarness } from '@angular/material/input/testing';

import { OrgSettingsRoleComponent } from './org-settings-role.component';

import { OrganizationSettingsModule } from '../../organization-settings.module';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { fakeRole } from '../../../../entities/role/role.fixture';
import { Role } from '../../../../entities/role/role';

describe('OrgSettingsRoleComponent', () => {
  const roleScope = 'ORGANIZATION';
  const role = 'USER';

  let fixture: ComponentFixture<OrgSettingsRoleComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, OrganizationSettingsModule],
      providers: [
        { provide: UIRouterState, useValue: {} },
        { provide: UIRouterStateParams, useValue: { roleScope, role } },
      ],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(OrgSettingsRoleComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should update role', async () => {
    const role = fakeRole({ id: 'roleId' });
    expectRoleGetRequest(role);

    const saveBar = await loader.getHarness(GioSaveBarHarness);

    const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=description]' }));
    await descriptionInput.setValue('New description');

    expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
    await saveBar.clickSubmit();

    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/${role.scope}/roles/${role.name}`,
      method: 'PUT',
    });
    expect(req.request.body).toEqual({ ...role, description: 'New description' });
    // No flush to stop test here
  });

  it('should disable form with a system role', async () => {
    const role = fakeRole({ id: 'roleId', system: true });
    expectRoleGetRequest(role);

    const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=description]' }));
    expect(await descriptionInput.isDisabled()).toEqual(true);
  });

  function expectRoleGetRequest(role: Role) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/${role.scope}/roles/${role.name}`, method: 'GET' })
      .flush(role);
    fixture.detectChanges();
  }
});
