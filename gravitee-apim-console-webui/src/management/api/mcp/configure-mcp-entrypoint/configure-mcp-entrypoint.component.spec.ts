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
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import { GioMonacoEditorHarness, GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ConfigureMcpEntrypointComponent } from './configure-mcp-entrypoint.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { Api, fakeProxyApiV4 } from '../../../../entities/management-api-v2';

describe('AddMcpEntrypointComponent', () => {
  let fixture: ComponentFixture<ConfigureMcpEntrypointComponent>;
  let httpTestingController: HttpTestingController;
  let harnessLoader: HarnessLoader;
  let routerSpy: jest.SpyInstance;

  const API_ID = 'api-id';
  const FAKE_MCP_ENTRYPOINT_SCHEMA = {
    $schema: 'http://json-schema.org/draft-07/schema#',
    type: 'object',
    properties: {
      tools: {
        type: 'string',
        title: 'MCP tools',
        description: 'The available tools description',
        format: 'gio-code-editor',
        gioConfig: {
          monacoEditorConfig: {
            language: 'json',
          },
        },
      },
      ssePath: {
        title: 'The SSE path for an AI agent',
        description:
          'The SSE path an AI agent will use to connect to the API. This path is appended to the API contextPath. Default is: /mcp/sse',
        type: 'string',
        default: '/mcp/sse',
      },
      messagesPath: {
        title: 'The messages path for an AI agent',
        description:
          'The messages path an AI agent will use to send messages to the API. This path is appended to the API contextPath. Default is: /mcp/messages',
        type: 'string',
        default: '/mcp/messages',
      },
    },
    required: ['tools'],
    additionalProperties: false,
  };

  const init = async (creationMode: boolean) => {
    await TestBed.configureTestingModule({
      imports: [ConfigureMcpEntrypointComponent, GioTestingModule, NoopAnimationsModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { data: { creationMode }, params: { apiId: API_ID } } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ConfigureMcpEntrypointComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    routerSpy = jest.spyOn(TestBed.inject(Router), 'navigate');

    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('When in creation mode', () => {
    beforeEach(async () => {
      await init(true);
      expectMcpSchema();
    });
    it('should show creation titles and schema form', async () => {
      expect(await getTitle()).toEqual('Enable MCP Entrypoint');
      expect(await getSubtitle()).toEqual('Configure the new MCP entrypoint');

      expect(await getToolsField()).toBeTruthy();

      const saveBar = await getSaveBar();
      expect(await saveBar.isVisible()).toEqual(true);
      expect(await saveBar.isSubmitButtonInvalid()).toEqual(true);
    });
    it('should save configuration', async () => {
      await setToolsFieldValue('["new tool": "value"]');
      await getSaveBar().then((saveBar) => saveBar.clickSubmit());

      const oldApi = fakeProxyApiV4({ id: API_ID });
      const newListeners = [...oldApi.listeners];
      const httpListener = newListeners[0];
      httpListener.entrypoints = [
        ...httpListener.entrypoints,
        { type: 'mcp', configuration: { messagesPath: '/mcp/messages', ssePath: '/mcp/sse', tools: '["new tool": "value"]' } },
      ];

      const newApi = fakeProxyApiV4({ id: API_ID, listeners: newListeners });

      expectUpdateApiCalls(oldApi, newApi);

      expect(routerSpy).toHaveBeenCalledWith(['..'], expect.anything());
    });
  });

  describe('When in edit mode', () => {
    const API = fakeProxyApiV4({
      id: API_ID,
      listeners: [
        {
          type: 'HTTP',
          entrypoints: [
            {
              type: 'http-proxy',
            },
            {
              type: 'mcp',
              configuration: {
                messagesPath: '/mcp/messages',
                ssePath: '/mcp/sse',
                tools: '["cats": "rule"]',
              },
            },
          ],
        },
      ],
    });
    beforeEach(async () => {
      await init(false);
      expectMcpSchema();
      expectGetApi(API);
    });

    it('should show edit titles and schema form', async () => {
      expect(await getTitle()).toEqual('Edit MCP Entrypoint');
      expect(await getSubtitle()).toEqual('Configure the MCP entrypoint');
      expect(await getToolsFieldValue()).toEqual('["cats": "rule"]');

      const saveBar = await getSaveBar();
      expect(await saveBar.isVisible()).toEqual(false);
    });

    it('should save updated configuration', async () => {
      await setToolsFieldValue('["dogs": "drool"]');
      await getSaveBar().then((saveBar) => saveBar.clickSubmit());

      const newListeners = [...API.listeners];
      const httpListener = newListeners[0];
      const mcpEntrypoint = httpListener.entrypoints.find((entrypoint) => entrypoint.type === 'mcp');
      mcpEntrypoint.configuration = {
        messagesPath: '/mcp/messages',
        ssePath: '/mcp/sse',
        tools: '["dogs": "drool"]',
      };

      const newApi = { ...API, listeners: newListeners };

      expectUpdateApiCalls(API, newApi);

      expect(routerSpy).toHaveBeenCalledWith(['..'], expect.anything());
    });
  });

  /**
   * HTTP expect calls
   */
  function expectMcpSchema(): void {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/entrypoints/mcp/schema`, method: 'GET' })
      .flush(FAKE_MCP_ENTRYPOINT_SCHEMA);
    fixture.detectChanges();
  }
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
    return harnessLoader.getHarness(MatCardHarness).then((card) => card.getTitleText());
  }
  async function getSubtitle(): Promise<string> {
    return harnessLoader.getHarness(MatCardHarness).then((card) => card.getSubtitleText());
  }
  async function getToolsField(): Promise<MatFormFieldHarness> {
    return harnessLoader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'MCP tools *' }));
  }
  async function getToolsFieldValue(): Promise<string> {
    const toolsField = await getToolsField();
    await toolsField.host().then((host) => {
      host.click();
    });

    const monacoCodeEditor = await harnessLoader.getHarness(GioMonacoEditorHarness);
    return await monacoCodeEditor.getValue();
  }
  async function setToolsFieldValue(value: string): Promise<void> {
    const toolsField = await getToolsField();
    await toolsField.host().then((host) => {
      host.click();
    });

    const monacoCodeEditor = await harnessLoader.getHarness(GioMonacoEditorHarness);
    await monacoCodeEditor.setValue(value);
  }
  async function getSaveBar(): Promise<GioSaveBarHarness> {
    return harnessLoader.getHarness(GioSaveBarHarness);
  }
});
