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

import { PolicyStudioDebugInspectorComponent } from './policy-studio-debug-inspector.component';

import { fakeRequestDebugStep } from '../../models/DebugStep.fixture';
import { PolicyStudioDebugModule } from '../../policy-studio-debug.module';

describe('PolicyStudioDebugInspectorComponent', () => {
  let fixture: ComponentFixture<PolicyStudioDebugInspectorComponent>;
  let component: PolicyStudioDebugInspectorComponent;

  const inputDebugStep = fakeRequestDebugStep();
  const outputDebugStep = fakeRequestDebugStep({
    policyOutput: {
      headers: {
        'content-length': ['3476'],
      },
    },
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PolicyStudioDebugModule],
    }).compileComponents();

    fixture = TestBed.createComponent(PolicyStudioDebugInspectorComponent);

    component = fixture.componentInstance;
    component.inputDebugStep = inputDebugStep;
    component.outputDebugStep = outputDebugStep;
    fixture.detectChanges();
  });

  it('should build tree nodes', () => {
    const treeNodes = component.buildTreeNodes();

    expect(treeNodes).not.toBeNull();
    expect(treeNodes.length).toBeGreaterThanOrEqual(3);
    expect(treeNodes[0].name).toEqual('HTTP properties');
    expect(treeNodes[0].children.length).toBeGreaterThanOrEqual(3);
    expect(treeNodes[0].children[0].name).toEqual('Path params');
    expect(treeNodes[0].children[0].type).toBeUndefined();
    expect(treeNodes[0].children[0].children.length).toEqual(1);
    expect(treeNodes[0].children[1].name).toEqual('Method');
    expect(treeNodes[0].children[1].type).toBeUndefined();
    expect(treeNodes[0].children[1].children[0].type).toEqual('text');
    expect(treeNodes[1].name).toEqual('HTTP headers');
    expect(treeNodes[1].children[0].type).toEqual('table');
    expect(treeNodes[1].children[0].input).toBeDefined();
    expect(treeNodes[1].children[0].output).toBeDefined();
    expect(treeNodes[2].name).toEqual('HTTP body');
    expect(treeNodes[2].children[0].type).toEqual('body');
    expect(treeNodes[2].children[0].input).toBeDefined();
    expect(treeNodes[2].children[0].output).toBeDefined();
  });
});
