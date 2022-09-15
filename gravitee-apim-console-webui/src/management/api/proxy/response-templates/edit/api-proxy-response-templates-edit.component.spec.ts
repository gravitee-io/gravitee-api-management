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
import { MatInputHarness } from '@angular/material/input/testing';

import { ApiProxyResponseTemplatesEditComponent } from './api-proxy-response-templates-edit.component';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { ApiProxyResponseTemplatesModule } from '../api-proxy-response-templates.module';
import { UIRouterStateParams, UIRouterState, CurrentUserService } from '../../../../../ajs-upgraded-providers';
import { User } from '../../../../../entities/user';
import { fakeApi } from '../../../../../entities/api/Api.fixture';
import { Api } from '../../../../../entities/api';

describe('ApiProxyResponseTemplatesComponent', () => {
  const API_ID = 'apiId';
  const fakeUiRouter = { go: jest.fn() };

  let fixture: ComponentFixture<ApiProxyResponseTemplatesEditComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const currentUser = new User();
  currentUser.userPermissions = ['api-response_templates-c', 'api-response_templates-u', 'api-response_templates-d'];

  const initTestingComponent = () => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiProxyResponseTemplatesModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID } },
        { provide: UIRouterState, useValue: fakeUiRouter },
        { provide: CurrentUserService, useValue: { currentUser } },
      ],
    });
    fixture = TestBed.createComponent(ApiProxyResponseTemplatesEditComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('creation mode', () => {
    beforeEach(() => {
      initTestingComponent();
    });

    it('should add new response template', async () => {
      const api = fakeApi({
        id: API_ID,
      });
      expectApiGetRequest(api);

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(true);

      const keyInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="key"]' }));
      expect(await keyInput.isDisabled()).toEqual(false);
      await keyInput.setValue('newTemplateKey');

      const acceptHeaderInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="acceptHeader"]' }));
      await acceptHeaderInput.setValue('application/json');

      const statusCodeInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="statusCode"]' }));
      await statusCodeInput.setValue('200');

      // const headersInput = await loader.getHarness(GioFormHeadersHarness.with({ selector: '[formControlName="headers"]' }));
      // TODO: When ui-particles is released

      const bodyInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="body"]' }));
      await bodyInput.setValue('newTemplateBody');

      const saveButton = await loader.getHarness(GioSaveBarHarness);
      await saveButton.clickSubmit();

      // Expect fetch api and update
      expectApiGetRequest(api);
      const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}` });
      expect(req.request.body.response_templates).toStrictEqual({
        ...api.response_templates,
        newTemplateKey: { 'application/json': { status: 200, body: 'newTemplateBody' } },
      });
    });
  });

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }
});
