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

import { PolicyStudioDebugInspectorTableComponent } from './policy-studio-debug-inspector-table.component';

import { PolicyStudioDebugModule } from '../../../policy-studio-debug.module';

describe('PolicyStudioDebugInspectorTableComponent', () => {
  let fixture: ComponentFixture<PolicyStudioDebugInspectorTableComponent>;
  let component: PolicyStudioDebugInspectorTableComponent;

  let input: Record<string, string | Array<string> | boolean>;
  let output: Record<string, string | Array<string> | boolean>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PolicyStudioDebugModule],
    }).compileComponents();

    fixture = TestBed.createComponent(PolicyStudioDebugInspectorTableComponent);

    component = fixture.componentInstance;
    component.input = input;
    component.output = output;
    fixture.detectChanges();
  });

  it('should build row items with empty input/output', () => {
    const rowItems = component.buildRowItems();
    expect(rowItems).toEqual([]);
  });

  it('should build row items with only with output', () => {
    component.output = {
      bb: 'cc',
      aa: '11',
    };
    fixture.detectChanges();

    const rowItems = component.buildRowItems();

    expect(rowItems).toEqual([
      {
        diffClass: 'added',
        inputValue: '',
        key: 'aa',
        outputValue: '11',
      },
      {
        diffClass: 'added',
        inputValue: '',
        key: 'bb',
        outputValue: 'cc',
      },
    ]);
  });

  it('should build row items with only with input/output', () => {
    component.input = {
      Cc: 'cc',
    };

    component.output = {
      bb: '22',
      aa: '11',
    };
    fixture.detectChanges();

    const rowItems = component.buildRowItems();

    expect(rowItems).toEqual([
      {
        diffClass: 'added',
        inputValue: '',
        key: 'aa',
        outputValue: '11',
      },
      {
        diffClass: 'added',
        inputValue: '',
        key: 'bb',
        outputValue: '22',
      },
      {
        diffClass: 'deleted',
        inputValue: 'cc',
        key: 'Cc',
        outputValue: '',
      },
    ]);
  });
});
