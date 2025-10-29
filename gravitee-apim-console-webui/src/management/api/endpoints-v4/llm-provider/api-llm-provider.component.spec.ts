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
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ApiLlmProviderComponent } from './api-llm-provider.component';
import { ApiLlmProviderModule } from './api-llm-provider.module';
import { ApiLlmProviderHarness } from './api-llm-provider.harness';

import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { GioTestingPermission, GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { ConnectorPluginsV2Service } from '../../../../services-ngx/connector-plugins-v2.service';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { ApiV4, fakeApiV4 } from '../../../../entities/management-api-v2';

describe('ApiProviderComponent', () => {
  const API_ID = 'api-id';
  let fixture: ComponentFixture<ApiLlmProviderComponent>;
  let componentHarness: ApiLlmProviderHarness;
  let httpTestingController: HttpTestingController;
  let routerNavigateSpy: jest.SpyInstance;

  const fakeSnackBarService = {
    error: jest.fn(),
    success: jest.fn(),
  };

  const initComponent = async (
    api: ApiV4,
    routerParams: unknown = { apiId: API_ID, providerIndex: 'new' },
    permissions: GioTestingPermission = ['api-definition-r', 'api-definition-u', 'api-definition-c'],
  ) => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, MatIconTestingModule, ApiLlmProviderModule, RouterTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              params: routerParams,
            },
          },
        },
        {
          provide: GioTestingPermissionProvider,
          useValue: permissions,
        },
        {
          provide: SnackBarService,
          useValue: fakeSnackBarService,
        },
        ApiV2Service,
        ConnectorPluginsV2Service,
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true,
          isTabbable: () => true,
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiLlmProviderComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiLlmProviderHarness);

    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate');

    fixture.detectChanges();

    await fixture.whenStable();

    expectApiGetRequest(api);
    expectEndpointSchemaGetRequest('llm-proxy');
    expectEndpointsSharedConfigurationSchemaGetRequest('llm-proxy');
  };

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('should create new provider', () => {
    it('should add a new provider', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        endpointGroups: [],
      });

      await initComponent(apiV4, { apiId: API_ID });

      expect(await componentHarness.isSaveButtonDisabled()).toBeTruthy();

      await componentHarness.setProviderName('Test Provider');
      fixture.componentInstance.formGroup.get('configuration')!.setValue({ provider: 'OPEN_AI_COMPATIBLE' });
      fixture.componentInstance.formGroup.get('sharedConfigurationOverride')!.setValue({ apiKey: 'test-key' });

      fixture.componentInstance.formGroup.get('configuration')!.setErrors(null);
      fixture.componentInstance.formGroup.get('sharedConfigurationOverride')!.setErrors(null);
      fixture.componentInstance.formGroup.updateValueAndValidity();

      expect(await componentHarness.isSaveButtonDisabled()).toBeFalsy();
      await componentHarness.clickSaveButton();

      expectApiGetRequest(apiV4);

      const updatedApi: ApiV4 = {
        ...apiV4,
        endpointGroups: [
          {
            name: 'Test Provider',
            type: 'llm-proxy',
            sharedConfiguration: { apiKey: 'test-key' },
            endpoints: [
              {
                name: 'Test Provider default endpoint',
                type: 'llm-proxy',
                inheritConfiguration: true,
                configuration: { provider: 'OPEN_AI_COMPATIBLE' },
                sharedConfigurationOverride: { apiKey: 'test-key' },
              },
            ],
          },
        ],
      };
      expectApiPutRequest(updatedApi);

      expect(fakeSnackBarService.success).toHaveBeenCalledWith('Provider Test Provider created!');
      expect(routerNavigateSpy).toHaveBeenCalledWith(['../../'], { relativeTo: expect.anything() });
    });
  });

  describe('should update existing provider', () => {
    it('should edit and save an existing provider', async () => {
      const existingProvider = {
        name: 'Existing Provider',
        type: 'llm-proxy',
        endpoints: [
          {
            name: 'Existing Provider',
            type: 'llm-proxy',
            configuration: { provider: 'ANTHROPIC' },
            sharedConfigurationOverride: { apiKey: 'existing-key' },
          },
        ],
      };

      const apiV4 = fakeApiV4({
        id: API_ID,
        endpointGroups: [existingProvider],
      });

      await initComponent(apiV4, { apiId: API_ID, providerIndex: 0 });

      expect(await componentHarness.getProviderName()).toBe('Existing Provider');

      await componentHarness.setProviderName('Updated Provider');
      fixture.componentInstance.formGroup.get('configuration')!.setValue({ provider: 'OPEN_AI_COMPATIBLE' });
      fixture.componentInstance.formGroup.get('sharedConfigurationOverride')!.setValue({ apiKey: 'updated-key' });

      fixture.componentInstance.formGroup.get('configuration')!.setErrors(null);
      fixture.componentInstance.formGroup.get('sharedConfigurationOverride')!.setErrors(null);
      fixture.componentInstance.formGroup.updateValueAndValidity();

      await componentHarness.clickSaveButton();

      expectApiGetRequest(apiV4);

      const updatedApi: ApiV4 = {
        ...apiV4,
        endpointGroups: [
          {
            ...existingProvider,
            name: 'Updated Provider',
            sharedConfiguration: { apiKey: 'updated-key' },
            endpoints: [
              {
                ...existingProvider.endpoints[0],
                name: 'Updated Provider default endpoint',
                configuration: { provider: 'OPEN_AI_COMPATIBLE' },
                sharedConfigurationOverride: { apiKey: 'updated-key' },
              },
            ],
          },
        ],
      };
      expectApiPutRequest(updatedApi);

      expect(fakeSnackBarService.success).toHaveBeenCalledWith('Provider successfully updated!');
      expect(routerNavigateSpy).toHaveBeenCalledWith(['../../'], { relativeTo: expect.anything() });
    });
  });

  function expectEndpointsSharedConfigurationSchemaGetRequest(id: string) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/endpoints/${id}/shared-configuration-schema`, method: 'GET' })
      .flush({
        $schema: 'http://json-schema.org/draft-07/schema#',
        type: 'object',
        properties: {
          apiKey: {
            title: 'API Key',
            description: 'API Key for the provider',
            type: 'string',
          },
        },
        required: ['apiKey'],
      });
    fixture.detectChanges();
  }

  function expectEndpointSchemaGetRequest(id: string) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/endpoints/${id}/schema`, method: 'GET' }).flush({
      $schema: 'http://json-schema.org/draft-07/schema#',
      type: 'object',
      properties: {
        provider: {
          title: 'Provider',
          description: 'LLM Provider',
          type: 'string',
        },
      },
      required: ['provider'],
    });
    fixture.detectChanges();
  }

  function expectApiGetRequest(api: ApiV4) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  function expectApiPutRequest(api: ApiV4) {
    const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'PUT' });
    expect(req.request.body).toStrictEqual(api);
    req.flush(api);
  }
});
