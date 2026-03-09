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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { McpToolComponent } from './mcp-tool.component';
import { McpToolHarness } from './mcp-tool.harness';
import { McpTool } from '../../entities/api/mcp';

describe('McpToolComponent', () => {
  let fixture: ComponentFixture<McpToolComponent>;
  let mcpToolHarness: McpToolHarness;

  const init = async (mcpTool: McpTool) => {
    await TestBed.configureTestingModule({
      imports: [McpToolComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(McpToolComponent);
    fixture.componentRef.setInput('tool', mcpTool);
    mcpToolHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, McpToolHarness);
    fixture.detectChanges();
  };

  it('should show the mcp tool information for defined tool', async () => {
    await init({
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
          required: ['name'],
        },
      },
    });

    const title = await mcpToolHarness.getTitleContent();
    expect(title).toBeTruthy();
    expect(title).toEqual('Cats rule tool');

    const description = await mcpToolHarness.getDescriptionContent();
    expect(description).toBeTruthy();
    expect(description).toEqual('MCP Tool Description');

    const content = await mcpToolHarness.getTextContent();
    expect(content).toContain('Cats rule tool');
    expect(content).toContain('MCP Tool Description');
    expect(content).toContain('Tool Name');
  });

  it('should handle when tool does not have name or description attributes', async () => {
    await init({
      foo: 'bar',
      bar: 'foo',
    });

    const title = await mcpToolHarness.getTitleContent();
    expect(title).toEqual('MCP Tool');

    const description = await mcpToolHarness.getDescriptionContent();
    expect(description).toBeFalsy();

    const content = await mcpToolHarness.getTextContent();
    expect(content).toContain('"foo": "bar",');
    expect(content).toContain('"bar": "foo"');
  });
});
