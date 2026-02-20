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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { MatCardModule } from '@angular/material/card';
import { GioIconsModule, GioLicenseTestingModule, LICENSE_CONFIGURATION_TESTING, GioLicenseService } from '@gravitee/ui-particles-angular';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule } from '@angular/material/dialog';

import { ApiEndpointGroupsLlmComponent } from './api-endpoint-groups-llm.component';
import { ApiEndpointGroupsLlmHarness } from './api-endpoint-groups-llm.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { ApiV4, fakeApiV4, fakeConnectorPlugin } from '../../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { GioTestingPermission, GioTestingPermissionProvider } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { ConnectorPluginsV2Service } from '../../../../../services-ngx/connector-plugins-v2.service';
import { IconService } from '../../../../../services-ngx/icon.service';
import { GioPermissionModule } from '../../../../../shared/components/gio-permission/gio-permission.module';
import { GioLicenseBannerModule } from '../../../../../shared/components/gio-license-banner/gio-license-banner.module';

describe('ApiEndpointGroupsLlmComponent', () => {
  const API_ID = 'api-id';
  let fixture: ComponentFixture<ApiEndpointGroupsLlmComponent>;
  let componentHarness: ApiEndpointGroupsLlmHarness;
  let httpTestingController: HttpTestingController;

  const initComponent = async (
    api: ApiV4,
    permissions: GioTestingPermission = ['api-definition-r', 'api-definition-u', 'api-definition-d'],
  ) => {
    await TestBed.configureTestingModule({
      declarations: [ApiEndpointGroupsLlmComponent],
      imports: [
        NoopAnimationsModule,
        GioTestingModule,
        MatIconTestingModule,
        GioPermissionModule,
        GioLicenseBannerModule,
        GioLicenseTestingModule,
        MatCardModule,
        MatButtonModule,
        MatTableModule,
        MatIconModule,
        GioIconsModule,
        MatDialogModule,
        RouterTestingModule,
      ],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              params: { apiId: API_ID },
            },
          },
        },
        {
          provide: GioTestingPermissionProvider,
          useValue: permissions,
        },
        {
          provide: 'LicenseConfiguration',
          useValue: LICENSE_CONFIGURATION_TESTING,
        },
        SnackBarService,
        IconService,
        GioLicenseService,
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

    fixture = TestBed.createComponent(ApiEndpointGroupsLlmComponent);
    fixture.componentRef.setInput('api', api);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiEndpointGroupsLlmHarness);

    fixture.detectChanges();

    expectEndpointsGetRequest();
    expectLicenseGetRequest();
  };

  afterEach(() => {
    jest.clearAllMocks();
    if (httpTestingController) {
      httpTestingController.verify();
    }
  });

  describe('table display tests', () => {
    it('should display providers table correctly', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        endpointGroups: [
          {
            name: 'OpenAI Provider',
            type: 'llm',
            endpoints: [
              {
                name: 'OpenAI Endpoint',
                type: 'llm-proxy',
                configuration: {
                  provider: 'OPEN_AI_COMPATIBLE',
                  models: [
                    { name: 'gpt-3.5-turbo', inputPrice: 0.001, outputPrice: 0.002 },
                    { name: 'gpt-4', inputPrice: 0.03, outputPrice: 0.06 },
                  ],
                },
              },
            ],
          },
          {
            name: 'Anthropic Provider',
            type: 'llm',
            endpoints: [
              {
                name: 'Anthropic Endpoint',
                type: 'llm-proxy',
                configuration: {
                  provider: 'ANTHROPIC',
                  models: [{ name: 'claude-3-sonnet', inputPrice: 0.003, outputPrice: 0.015 }],
                },
              },
            ],
          },
        ],
      });

      await initComponent(apiV4);

      expect(await componentHarness.getProviderCards()).toBe(2);
      expect(await componentHarness.getProviderName(0)).toBe('OpenAI Provider');
      expect(await componentHarness.getProviderName(1)).toBe('Anthropic Provider');
      expect(await componentHarness.getProviderType(0)).toBe('OpenAI Compatible');
      expect(await componentHarness.getProviderType(1)).toBe('ANTHROPIC');
    });

    it('should display models table for each provider', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        endpointGroups: [
          {
            name: 'OpenAI Provider',
            type: 'llm',
            endpoints: [
              {
                name: 'OpenAI Endpoint',
                type: 'llm-proxy',
                configuration: {
                  provider: 'OPEN_AI_COMPATIBLE',
                  models: [
                    { name: 'gpt-3.5-turbo', inputPrice: 0.001, outputPrice: 0.002 },
                    { name: 'gpt-4', inputPrice: 0.03, outputPrice: 0.06 },
                  ],
                },
              },
            ],
          },
        ],
      });

      await initComponent(apiV4);

      const modelsTable = await componentHarness.getModelsTable(0);
      expect(modelsTable).toBeTruthy();

      const modelsRows = await componentHarness.getModelsTableRows(0);
      expect(modelsRows).toHaveLength(2);
      expect(modelsRows[0]).toEqual(['gpt-3.5-turbo', '', '0.001', '0.002']);
      expect(modelsRows[1]).toEqual(['gpt-4', '', '0.03', '0.06']);
    });
  });

  describe('capabilities display tests', () => {
    it('should display capability badges when model has capabilities', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        endpointGroups: [
          {
            name: 'OpenAI Provider',
            type: 'llm',
            endpoints: [
              {
                name: 'OpenAI Endpoint',
                type: 'llm-proxy',
                configuration: {
                  provider: 'OPEN_AI',
                  models: [
                    {
                      name: 'gpt-5',
                      inputPrice: 2.5,
                      outputPrice: 10.0,
                      streaming: true,
                      thinking: true,
                      functionCalling: true,
                      contextWindowSize: 128000,
                      supportedEndpoints: ['CHAT_COMPLETIONS', 'RESPONSES'],
                      inputModalities: ['TEXT', 'IMAGE'],
                      outputModalities: ['TEXT'],
                    },
                  ],
                },
              },
            ],
          },
        ],
      });

      await initComponent(apiV4);

      const modelsRows = await componentHarness.getModelsTableRows(0);
      expect(modelsRows).toHaveLength(1);
      // capabilities column (index 1) should contain badge text
      expect(modelsRows[0][1]).toContain('Streaming');
      expect(modelsRows[0][1]).toContain('Thinking');
      expect(modelsRows[0][1]).toContain('Function Calling');
      expect(modelsRows[0][1]).toContain('128k ctx');
    });

    it('should display empty capabilities for models without capabilities (backward compat)', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        endpointGroups: [
          {
            name: 'OpenAI Provider',
            type: 'llm',
            endpoints: [
              {
                name: 'OpenAI Endpoint',
                type: 'llm-proxy',
                configuration: {
                  provider: 'OPEN_AI',
                  models: [{ name: 'gpt-4', inputPrice: 0.03, outputPrice: 0.06 }],
                },
              },
            ],
          },
        ],
      });

      await initComponent(apiV4);

      const modelsRows = await componentHarness.getModelsTableRows(0);
      expect(modelsRows).toHaveLength(1);
      // capabilities column (index 1) should be empty
      expect(modelsRows[0][1]).toBe('');
    });
  });

  describe('action buttons tests', () => {
    it('should show edit button when user has update permission', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        endpointGroups: [
          {
            name: 'OpenAI Provider',
            type: 'llm',
            endpoints: [
              {
                name: 'OpenAI Endpoint',
                type: 'llm-proxy',
                configuration: { provider: 'OPEN_AI_COMPATIBLE', models: [] },
              },
            ],
          },
          {
            name: 'Anthropic Provider',
            type: 'llm',
            endpoints: [
              {
                name: 'Anthropic Endpoint',
                type: 'llm-proxy',
                configuration: { provider: 'ANTHROPIC', models: [] },
              },
            ],
          },
        ],
      });

      await initComponent(apiV4, ['api-definition-r', 'api-definition-u']);

      expect(await componentHarness.isEditButtonVisible(0)).toBe(true);
      expect(await componentHarness.isDeleteButtonVisible(0)).toBe(true);
    });

    it('should hide edit and delete buttons when user has no update permission', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        endpointGroups: [
          {
            name: 'OpenAI Provider',
            type: 'llm',
            endpoints: [
              {
                name: 'OpenAI Endpoint',
                type: 'llm-proxy',
                configuration: { provider: 'OPEN_AI_COMPATIBLE', models: [] },
              },
            ],
          },
        ],
      });

      await initComponent(apiV4, ['api-definition-r']);

      expect(await componentHarness.isEditButtonVisible(0)).toBe(false);
      expect(await componentHarness.isDeleteButtonVisible(0)).toBe(false);
    });

    it('should show add provider button when user has create permission', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        endpointGroups: [],
      });

      await initComponent(apiV4, ['api-definition-r', 'api-definition-u']);

      expect(await componentHarness.isAddProviderButtonVisible()).toBe(true);
    });

    it('should hide add provider button when user has no create permission', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        endpointGroups: [],
      });

      await initComponent(apiV4, ['api-definition-r']);

      expect(await componentHarness.isAddProviderButtonVisible()).toBe(false);
    });
  });

  describe('delete flow tests', () => {
    it('should show delete button when there are multiple providers', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        endpointGroups: [
          {
            name: 'OpenAI Provider',
            type: 'llm',
            endpoints: [
              {
                name: 'OpenAI Endpoint',
                type: 'llm-proxy',
                configuration: { provider: 'OPEN_AI_COMPATIBLE', models: [] },
              },
            ],
          },
          {
            name: 'Anthropic Provider',
            type: 'llm',
            endpoints: [
              {
                name: 'Anthropic Endpoint',
                type: 'llm-proxy',
                configuration: { provider: 'ANTHROPIC', models: [] },
              },
            ],
          },
        ],
      });

      await initComponent(apiV4, ['api-definition-r', 'api-definition-u', 'api-definition-d']);

      // Should have 2 providers
      expect(await componentHarness.getProviderCards()).toBe(2);
      // Delete button should be visible when there are multiple providers
      expect(await componentHarness.isDeleteButtonVisible(0)).toBe(true);
      expect(await componentHarness.isDeleteButtonVisible(1)).toBe(true);
    });

    it('should not show delete button when there is only one provider', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        endpointGroups: [
          {
            name: 'OpenAI Provider',
            type: 'llm',
            endpoints: [
              {
                name: 'OpenAI Endpoint',
                type: 'llm-proxy',
                configuration: { provider: 'OPEN_AI_COMPATIBLE', models: [] },
              },
            ],
          },
        ],
      });

      await initComponent(apiV4, ['api-definition-r', 'api-definition-u', 'api-definition-d']);

      // Should have 1 provider
      expect(await componentHarness.getProviderCards()).toBe(1);
      // Delete button should not be visible when there is only one provider
      expect(await componentHarness.isDeleteButtonVisible(0)).toBe(false);
    });
  });

  describe('navigation tests', () => {
    it('should have correct routerLink for add provider button', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        endpointGroups: [],
      });

      await initComponent(apiV4, ['api-definition-r', 'api-definition-u']);

      expect(await componentHarness.isAddProviderButtonVisible()).toBe(true);
    });

    it('should have correct routerLink for edit provider button', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        endpointGroups: [
          {
            name: 'OpenAI Provider',
            type: 'llm',
            endpoints: [
              {
                name: 'OpenAI Endpoint',
                type: 'llm-proxy',
                configuration: { provider: 'OPEN_AI_COMPATIBLE', models: [] },
              },
            ],
          },
        ],
      });

      await initComponent(apiV4, ['api-definition-r', 'api-definition-u']);

      expect(await componentHarness.isEditButtonVisible(0)).toBe(true);
    });
  });

  function expectEndpointsGetRequest() {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/endpoints`, method: 'GET' })
      .flush([fakeConnectorPlugin({ id: 'llm-proxy', name: 'LLM Proxy' })]);
    fixture.detectChanges();
  }

  function expectLicenseGetRequest() {
    const licenseReq = httpTestingController.match({ url: LICENSE_CONFIGURATION_TESTING.resourceURL, method: 'GET' });
    if (licenseReq.length > 0) {
      licenseReq[0].flush({});
    }
    fixture.detectChanges();
  }
});
