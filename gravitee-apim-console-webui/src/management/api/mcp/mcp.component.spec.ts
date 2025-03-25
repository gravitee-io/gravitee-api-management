<<<<<<< HEAD
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

import { McpComponent } from './mcp.component';
import { McpHarness } from './mcp.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { ApiV4, ConnectorPlugin, fakeApiV4, fakeConnectorPlugin } from '../../../entities/management-api-v2';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';

describe('McpComponent', () => {
  let fixture: ComponentFixture<McpComponent>;
  let componentHarness: McpHarness;
  let httpTestingController: HttpTestingController;
  let routerSpy: jest.SpyInstance;

  const API_ID = 'api-id';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GioTestingModule, McpComponent],
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
      expect(routerSpy).toHaveBeenCalledWith(['./add'], expect.anything());
    });

    it('should not navigate to adding the entrypoint if mcp entrypoint is missing', async () => {
      expectGetEntrypoints([]);

      const mcpEntrypointNotFound = await componentHarness.getMcpEntryPointNotFound();
      const enableMcpButton = await mcpEntrypointNotFound.getEnableMcpButton();
      expect(await enableMcpButton.isDisabled()).toBeTruthy();
    });
  });

  function expectGetApi(api: ApiV4): void {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  function expectGetEntrypoints(entrypoints: ConnectorPlugin[]) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/entrypoints`, method: 'GET' }).flush(entrypoints);
    fixture.detectChanges();
  }
=======
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { McpComponent } from './mcp.component';

describe('McpComponent', () => {
  let component: McpComponent;
  let fixture: ComponentFixture<McpComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [McpComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(McpComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
>>>>>>> 4fd3c69d69 (feat(console): manage mcp tools)
});
