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

import { ToolDisplayComponent } from './tool-display.component';
import { ToolDisplayHarness } from './tool-display.harness';

describe('ToolDisplayComponent', () => {
  let fixture: ComponentFixture<ToolDisplayComponent>;
  let harness: ToolDisplayHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToolDisplayComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ToolDisplayComponent);

    fixture.componentRef.setInput('tool', {
      name: 'Test Tool',
      description: 'A test tool',
      inputSchema: { type: 'object' },
    });

    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ToolDisplayHarness);
    fixture.detectChanges();
  });

  it('should display tool when provided', async () => {
    expect(await harness.getToolSchema()).toContain('Test Tool');
  });
});
