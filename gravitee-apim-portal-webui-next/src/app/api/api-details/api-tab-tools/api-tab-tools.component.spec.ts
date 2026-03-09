/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ApiTabToolsComponent } from './api-tab-tools.component';
import { McpToolHarness } from '../../../../components/mcp-tool/mcp-tool.harness';
import { Api } from '../../../../entities/api/api';
import { fakeApi } from '../../../../entities/api/api.fixtures';

describe('ApiTabToolsComponent', () => {
  let fixture: ComponentFixture<ApiTabToolsComponent>;
  let harnessLoader: HarnessLoader;

  beforeEach(async () => {});

  const init = async (api: Api) => {
    await TestBed.configureTestingModule({
      imports: [ApiTabToolsComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiTabToolsComponent);
    fixture.componentRef.setInput('api', api);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  };

  it('should show empty tools message when no tools are available', async () => {
    await init(
      fakeApi({
        mcp: {
          mcpPath: '/mcp',
          tools: [],
        },
      }),
    );
    fixture.detectChanges();

    const emptyMessage = fixture.nativeElement.querySelector('.api-tab-tools__empty');
    expect(emptyMessage).not.toBeNull();
    expect(emptyMessage.textContent.trim()).toContain('No tools available');
  });

  it('should show tools when available', async () => {
    await init(
      fakeApi({
        mcp: {
          mcpPath: '/mcp',
          tools: [
            {
              toolDefinition: {
                name: 'Cats rule tool',
                description: 'MCP Tool Description',
                inputSchema: {
                  type: 'object',
                  properties: {
                    name: {
                      type: 'string',
                      description: 'Tool Name',
                    },
                    arguments: {
                      type: 'string',
                    },
                  },
                  required: ['name'],
                },
              },
            },
            {
              toolDefinition: {
                foo: 'bar',
                bar: 'foo',
              },
            },
          ],
        },
      }),
    );

    const tools = await harnessLoader.getAllHarnesses(McpToolHarness);
    expect(tools.length).toBe(2);
    expect(await tools[0].getTitleContent()).toBe('Cats rule tool');
    expect(await tools[1].getTitleContent()).toBe('MCP Tool');
  });
});
