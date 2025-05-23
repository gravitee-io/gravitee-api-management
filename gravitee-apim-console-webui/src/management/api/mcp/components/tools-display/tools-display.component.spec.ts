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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ToolsDisplayComponent } from './tools-display.component';
import { ToolsDisplayHarness } from './tools-display.harness';

import { MCPToolDefinition } from '../../../../../entities/entrypoint/mcp';

describe('ToolsDisplayComponent', () => {
  let component: ToolsDisplayComponent;
  let fixture: ComponentFixture<ToolsDisplayComponent>;
  let harness: ToolsDisplayHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToolsDisplayComponent, NoopAnimationsModule],
      declarations: [],
    }).compileComponents();

    fixture = TestBed.createComponent(ToolsDisplayComponent);
    component = fixture.componentInstance;
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ToolsDisplayHarness);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display empty state message when no tools are provided', async () => {
    expect(await harness.isEmptyState()).toBe(true);
    expect(await harness.getEmptyStateMessage()).toContain('No tools are configured');
  });

  it('should display tools when provided', async () => {
    const mockTools: MCPToolDefinition[] = [
      {
        name: 'Test Tool',
        description: 'A test tool',
        inputSchema: { type: 'object' },
      },
    ];

    fixture.componentRef.setInput('tools', [mockTools]);
    fixture.detectChanges();

    expect(await harness.isEmptyState()).toBe(false);
    expect(await harness.getToolCount()).toBe(1);

    const tools = await harness.getTools();
    expect(tools[0]).toContain('Test Tool');
  });
});
