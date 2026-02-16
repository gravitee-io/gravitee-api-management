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
import { ActivatedRoute, Router } from '@angular/router';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatCardHarness } from '@angular/material/card/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { EnableMcpEntrypointComponent } from './enable-mcp-entrypoint.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { Api, fakeProxyApiV4 } from '../../../../entities/management-api-v2';
import { ConfigureMcpEntrypointHarness } from '../components/configure-mcp-entrypoint/configure-mcp-entrypoint.harness';

describe('EnableMcpEntrypointComponent', () => {
  let fixture: ComponentFixture<EnableMcpEntrypointComponent>;
  let httpTestingController: HttpTestingController;
  let harnessLoader: HarnessLoader;
  let routerSpy: jest.SpyInstance;

  const API_ID = 'api-id';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EnableMcpEntrypointComponent, GioTestingModule, NoopAnimationsModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { params: { apiId: API_ID } } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(EnableMcpEntrypointComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    routerSpy = jest.spyOn(TestBed.inject(Router), 'navigate');

    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should show creation titles and form', async () => {
    expect(await getTitle()).toEqual('Enable MCP Entrypoint');
    expect(await getSubtitle()).toEqual('Configure the new MCP entrypoint');

    const mcpConfigurationForm = await harnessLoader.getHarness(ConfigureMcpEntrypointHarness);
    expect(await mcpConfigurationForm.getMcpPathValue()).toEqual('/mcp');

    expect(await mcpConfigurationForm.hasTools()).toEqual(false);

    const saveBar = await getSaveBar();
    expect(await saveBar.isVisible()).toEqual(true);
    expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
  });

  it('should save configuration', async () => {
    const mcpConfigurationForm = await harnessLoader.getHarness(ConfigureMcpEntrypointHarness);
    await mcpConfigurationForm.setMcpPathValue('/cats-rule');

    await getSaveBar().then(saveBar => saveBar.clickSubmit());

    const oldApi = fakeProxyApiV4({ id: API_ID });
    const newApi = { ...oldApi };
    const httpListener = newApi.listeners[0];
    httpListener.entrypoints = [...httpListener.entrypoints, { type: 'mcp', configuration: { mcpPath: '/cats-rule', tools: [] } }];
    newApi.listeners[0] = httpListener;

    expectUpdateApiCalls(oldApi, newApi);

    expect(routerSpy).toHaveBeenCalledWith(['..'], expect.anything());
  });

  /**
   * HTTP expect calls
   */
  function expectGetApi(api: Api): void {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }
  function expectUpdateApiCalls(oldApi: Api, newApi: Api): void {
    expectGetApi(oldApi);

    const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${newApi.id}`, method: 'PUT' });
    expect(req.request.body).toEqual(newApi);
    req.flush(newApi);

    fixture.detectChanges();
  }

  /**
   * Get component elements
   */
  async function getTitle(): Promise<string> {
    return harnessLoader.getHarness(MatCardHarness).then(card => card.getTitleText());
  }
  async function getSubtitle(): Promise<string> {
    return harnessLoader.getHarness(MatCardHarness).then(card => card.getSubtitleText());
  }
  async function getSaveBar(): Promise<GioSaveBarHarness> {
    return harnessLoader.getHarness(GioSaveBarHarness);
  }
});
