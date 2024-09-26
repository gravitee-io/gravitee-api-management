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
import { GioFormHeadersHarness, GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { cloneDeep } from 'lodash';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { ActivatedRoute } from '@angular/router';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';

import { ApiResponseTemplatesEditComponent } from './api-response-templates-edit.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { ApiResponseTemplatesModule } from '../api-response-templates.module';
import { ApiV2, ApiV4, fakeApiV2, fakeApiV4 } from '../../../../entities/management-api-v2';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';

describe('ApiProxyResponseTemplatesComponent', () => {
  const API_ID = 'apiId';
  const apiV2 = fakeApiV2({
    id: API_ID,
    responseTemplates: {
      customKey: {
        '*/*': {
          statusCode: 400,
          body: '',
        },
      },
      DEFAULT: {
        '*/*': {
          statusCode: 400,
          body: '',
        },
        test: {
          statusCode: 400,
          body: '',
        },
      },
    },
  });
  const apiV4 = fakeApiV4({
    id: API_ID,
    responseTemplates: {
      customKey: {
        '*/*': {
          statusCode: 400,
          body: '',
        },
      },
      DEFAULT: {
        '*/*': {
          statusCode: 400,
          body: '',
        },
        test: {
          statusCode: 400,
          body: '',
        },
      },
    },
  });
  const kubernetesApiV4 = fakeApiV4({
    id: API_ID,
    definitionContext: {
      origin: 'KUBERNETES',
    },
    responseTemplates: {
      DEFAULT: {
        '*/*': {
          body: 'json',
          statusCode: 400,
        },
      },
    },
  });
  const kubernetesApiV2 = fakeApiV2({
    id: API_ID,
    definitionContext: {
      origin: 'KUBERNETES',
    },
    responseTemplates: {
      DEFAULT: {
        '*/*': {
          body: 'json',
          statusCode: 400,
        },
      },
    },
  });

  let fixture: ComponentFixture<ApiResponseTemplatesEditComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const initTestingComponent = (responseTemplateId?: string) => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ApiResponseTemplatesModule, MatIconTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID, responseTemplateId } } } },
        {
          provide: GioTestingPermissionProvider,
          useValue: ['api-response_templates-c', 'api-response_templates-u', 'api-response_templates-d'],
        },
      ],
    });
    fixture = TestBed.createComponent(ApiResponseTemplatesEditComponent);
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

    describe.each([{ api: apiV2 }, { api: apiV4 }])('With API $api.definitionVersion', ({ api }) => {
      it('should add new response template', async () => {
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

        const headersInput = await loader.getHarness(GioFormHeadersHarness.with({ selector: '[formControlName="headers"]' }));
        await headersInput.addHeader({ key: 'header1', value: 'value1' });

        const bodyInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="body"]' }));
        await bodyInput.setValue('newTemplateBody');

        const propagateErrorKeyToLogsToggle = await loader.getHarness(
          MatSlideToggleHarness.with({ selector: '[formControlName="propagateErrorKeyToLogs"]' }),
        );
        await propagateErrorKeyToLogsToggle.toggle();

        await saveBar.clickSubmit();

        // Expect fetch api and update
        expectApiGetRequest(api);
        const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}` });
        expect(req.request.body.responseTemplates).toEqual({
          ...api.responseTemplates,
          newTemplateKey: {
            'application/json': { statusCode: 200, headers: { header1: 'value1' }, propagateErrorKeyToLogs: true, body: 'newTemplateBody' },
          },
        });
      });

      it('should add new response template to existing template key', async () => {
        expectApiGetRequest(api);

        const saveBar = await loader.getHarness(GioSaveBarHarness);
        expect(await saveBar.isVisible()).toBe(true);

        const keyInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="key"]' }));
        expect(await keyInput.isDisabled()).toEqual(false);
        await keyInput.setValue('DEFAULT');

        const acceptHeaderInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="acceptHeader"]' }));
        await acceptHeaderInput.setValue('application/json');

        await saveBar.clickSubmit();

        // Expect fetch api and update
        expectApiGetRequest(api);
        const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}` });
        expect(req.request.body.responseTemplates).toEqual({
          ...api.responseTemplates,
          ['DEFAULT']: {
            ...api.responseTemplates['DEFAULT'],
            'application/json': { statusCode: 400, body: '', propagateErrorKeyToLogs: false },
          },
        });
      });
    });
  });

  describe('edition mode', () => {
    beforeEach(() => {
      initTestingComponent('DEFAULT-*/*');
    });

    describe.each([{ api: apiV2 }, { api: apiV4 }])('With API $api.definitionVersion', ({ api }) => {
      it('should edit response template', async () => {
        expectApiGetRequest(api);

        const saveBar = await loader.getHarness(GioSaveBarHarness);
        expect(await saveBar.isVisible()).toBe(false);

        const keyInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="key"]' }));
        expect(await keyInput.isDisabled()).toEqual(false);
        expect(await keyInput.getValue()).toEqual('DEFAULT');

        const acceptHeaderInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="acceptHeader"]' }));
        expect(await acceptHeaderInput.getValue()).toEqual('*/*');

        const statusCodeInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="statusCode"]' }));
        expect(await statusCodeInput.getValue()).toEqual('400');

        const headersInput = await loader.getHarness(GioFormHeadersHarness.with({ selector: '[formControlName="headers"]' }));
        await headersInput.addHeader({ key: 'header1', value: 'value1' });

        const bodyInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="body"]' }));
        await bodyInput.setValue('newTemplateBody');

        await saveBar.clickSubmit();

        // Expect fetch api and update
        expectApiGetRequest(api);
        const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}` });
        expect(req.request.body.responseTemplates).toEqual({
          ...api.responseTemplates,
          ['DEFAULT']: {
            ...api.responseTemplates['DEFAULT'],
            '*/*': {
              statusCode: 400,
              headers: {
                header1: 'value1',
              },
              body: 'newTemplateBody',
              propagateErrorKeyToLogs: false,
            },
          },
        });
      });

      it('should edit response template key and accept header', async () => {
        expectApiGetRequest(api);

        const saveBar = await loader.getHarness(GioSaveBarHarness);
        expect(await saveBar.isVisible()).toBe(false);

        const keyInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="key"]' }));
        expect(await keyInput.isDisabled()).toEqual(false);
        expect(await keyInput.getValue()).toEqual('DEFAULT');
        await keyInput.setValue('NewKey');

        const acceptHeaderInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="acceptHeader"]' }));
        await acceptHeaderInput.setValue('new/accept');

        await saveBar.clickSubmit();

        // Expect fetch api and update
        expectApiGetRequest(api);

        const expectedResponseTemplates = cloneDeep(api.responseTemplates);
        delete expectedResponseTemplates['DEFAULT']['*/*'];
        expectedResponseTemplates.NewKey = {
          'new/accept': { statusCode: 400, body: '', propagateErrorKeyToLogs: false },
        };

        const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}` });
        expect(req.request.body.responseTemplates).toEqual(expectedResponseTemplates);
      });

      it('should throw if new template key and accept header already exist', async () => {
        expectApiGetRequest(api);

        const saveBar = await loader.getHarness(GioSaveBarHarness);
        expect(await saveBar.isVisible()).toBe(false);

        const keyInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="key"]' }));
        const acceptHeaderInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="acceptHeader"]' }));

        // Fake api already have a template with key `DEFAULT` and accept header `test`
        await acceptHeaderInput.setValue('test');
        expect(await saveBar.isSubmitButtonInvalid()).toBe(true);

        // Ok no template with key `customKey` and accept header `test`
        await keyInput.setValue('customKey');
        expect(await saveBar.isSubmitButtonInvalid()).toBe(false);

        // Fake api already have a template with key `customKey` and accept header `*/*`
        await acceptHeaderInput.setValue('*/*');
        expect(await saveBar.isSubmitButtonInvalid()).toBe(true);
      });
    });

    describe.each([{ api: kubernetesApiV2 }, { api: kubernetesApiV4 }])(
      'With API $api.definitionVersion with KUBERNETES origin',
      ({ api }) => {
        it('should disable field when origin is kubernetes', async () => {
          expectApiGetRequest(api);

          const keyInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="key"]' }));
          expect(await keyInput.isDisabled()).toEqual(true);
          expect(await keyInput.getValue()).toEqual('DEFAULT');

          const acceptHeaderInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="acceptHeader"]' }));
          expect(await acceptHeaderInput.isDisabled()).toEqual(true);
          expect(await acceptHeaderInput.getValue()).toEqual('*/*');

          const statusCodeInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="statusCode"]' }));
          expect(await statusCodeInput.isDisabled()).toEqual(true);
          expect(await statusCodeInput.getValue()).toEqual('400');

          const headersInput = await loader.getHarness(GioFormHeadersHarness.with({ selector: '[formControlName="headers"]' }));
          expect(await headersInput.isDisabled()).toEqual(true);

          const bodyInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="body"]' }));
          expect(await bodyInput.isDisabled()).toEqual(true);
          expect(await bodyInput.getValue()).toEqual('json');
        });
      },
    );
  });

  function expectApiGetRequest(api: ApiV2 | ApiV4) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }
});
