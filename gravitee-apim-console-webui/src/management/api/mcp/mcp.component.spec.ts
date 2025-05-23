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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { McpComponent } from './mcp.component';
import { McpHarness } from './mcp.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { ApiV4, ConnectorPlugin, fakeApiV4, fakeConnectorPlugin, fakeProxyApiV4 } from '../../../entities/management-api-v2';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';

describe('McpComponent', () => {
  let fixture: ComponentFixture<McpComponent>;
  let componentHarness: McpHarness;
  let httpTestingController: HttpTestingController;
  let routerSpy: jest.SpyInstance;

  const API_ID = 'api-id';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GioTestingModule, McpComponent, NoopAnimationsModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              params: {
                apiId: API_ID,
              },
            },
          },
        },
        { provide: GioTestingPermissionProvider, useValue: ['api-definition-u'] },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(McpComponent);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, McpHarness);
    httpTestingController = TestBed.inject(HttpTestingController);
    routerSpy = jest.spyOn(TestBed.inject(Router), 'navigate');
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('when no mcp entrypoint is found', () => {
    beforeEach(async () => {
      expectGetApi(fakeApiV4({ id: API_ID, listeners: [{ type: 'HTTP', entrypoints: [{ type: 'http-proxy' }] }] }));
    });
    it('should display the mcp entrypoint not found message', async () => {
      expectGetEntrypoints([]);

      const mcpEntryPointNotFound = await componentHarness.getMcpEntryPointNotFound();
      expect(mcpEntryPointNotFound).toBeTruthy();
    });

    it('should show navigate to edit view after clicking Enable MCP button if MCP Entrypoint installed', async () => {
      expectGetEntrypoints([fakeConnectorPlugin({ id: 'mcp' })]);

      const mcpEntrypointNotFound = await componentHarness.getMcpEntryPointNotFound();
      const enableMcpButton = await mcpEntrypointNotFound.getEnableMcpButton();
      await enableMcpButton.click();

      expect(routerSpy).toHaveBeenCalledTimes(1);
      expect(routerSpy).toHaveBeenCalledWith(['./enable'], expect.anything());
    });

    it('should not navigate to adding the entrypoint if mcp entrypoint is missing', async () => {
      expectGetEntrypoints([]);

      const mcpEntrypointNotFound = await componentHarness.getMcpEntryPointNotFound();
      const enableMcpButton = await mcpEntrypointNotFound.getEnableMcpButton();
      expect(await enableMcpButton.isDisabled()).toBeTruthy();
    });
  });

  describe('when mcp entrypoint is found', () => {
    describe('when entrypoint configuration defined + empty tools', () => {
      const API = fakeProxyApiV4({
        id: API_ID,
        listeners: [
          {
            type: 'HTTP',
            entrypoints: [
              { type: 'http-proxy' },
              {
                type: 'mcp',
                configuration: {
                  mcpPath: '/cats-rule',
                  tools: [],
                },
              },
            ],
          },
        ],
      });
      beforeEach(async () => {
        expectGetApi(API);
      });
      it('should display the mcp entrypoint edit form', async () => {
        const mcpConfigurationForm = await componentHarness.getConfigureMcpEntrypoint();
        expect(await mcpConfigurationForm.getMcpPathValue()).toEqual('/cats-rule');
        expect(await mcpConfigurationForm.hasTools()).toEqual(false);
      });
      it('should edit mcp entrypoint form and submit', async () => {
        const mcpConfigurationForm = await componentHarness.getConfigureMcpEntrypoint();
        await mcpConfigurationForm.setMcpPathValue('/dogs-drool');

        const saveBar = await componentHarness.getSaveBar();
        expect(saveBar).toBeTruthy();

        await saveBar.clickSubmit();

        const newApi = { ...API };
        const httpListener = newApi.listeners[0];
        httpListener.entrypoints = [...httpListener.entrypoints, { type: 'mcp', configuration: { mcpPath: '/dogs-drool', tools: [] } }];
        newApi.listeners[0] = httpListener;

        expectUpdateApiCalls(API, newApi);
      });
    });
  });

  function expectGetApi(api: ApiV4): void {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  function expectUpdateApiCalls(oldApi: ApiV4, newApi: ApiV4): void {
    expectGetApi(oldApi);

    const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${newApi.id}`, method: 'PUT' });
    expect(req.request.body).toEqual(newApi);
    req.flush(newApi);

    fixture.detectChanges();
  }

  function expectGetEntrypoints(entrypoints: ConnectorPlugin[]) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/entrypoints`, method: 'GET' }).flush(entrypoints);
    fixture.detectChanges();
  }
});
