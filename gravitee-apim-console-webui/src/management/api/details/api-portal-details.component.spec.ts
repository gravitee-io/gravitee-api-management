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
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { ApiPortalDetailsModule } from './api-portal-details.module';
import { ApiPortalDetailsComponent } from './api-portal-details.component';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { Api } from '../../../entities/api';
import { fakeApi } from '../../../entities/api/Api.fixture';
import { UIRouterStateParams, CurrentUserService } from '../../../ajs-upgraded-providers';
import { User } from '../../../entities/user';

describe('ApiPortalDetailsComponent', () => {
  const API_ID = 'apiId';
  const currentUser = new User();
  currentUser.userPermissions = ['api-definition-u'];

  let fixture: ComponentFixture<ApiPortalDetailsComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiPortalDetailsModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID } },
        { provide: CurrentUserService, useValue: { currentUser } },
      ],
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ApiPortalDetailsComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should edit api details', async () => {
    const api = fakeApi({
      id: API_ID,
      name: '🐶 API',
      version: '1.0.0',
    });
    expectApiGetRequest(api);

    const saveBar = await loader.getHarness(GioSaveBarHarness);
    expect(await saveBar.isVisible()).toBe(false);

    const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
    expect(await nameInput.getValue()).toEqual('🐶 API');
    await nameInput.setValue('🦊 API');

    const versionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="version"]' }));
    expect(await versionInput.getValue()).toEqual('1.0.0');
    await versionInput.setValue('2.0.0');

    const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="description"]' }));
    expect(await descriptionInput.getValue()).toEqual('The whole universe in your hand.');
    await descriptionInput.setValue('🦊 API description');

    expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
    await saveBar.clickSubmit();

    // Expect fetch api and update
    expectApiGetRequest(api);
    const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}` });
    expect(req.request.body.name).toEqual('🦊 API');
    expect(req.request.body.version).toEqual('2.0.0');
    expect(req.request.body.description).toEqual('🦊 API description');
  });

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }
});
