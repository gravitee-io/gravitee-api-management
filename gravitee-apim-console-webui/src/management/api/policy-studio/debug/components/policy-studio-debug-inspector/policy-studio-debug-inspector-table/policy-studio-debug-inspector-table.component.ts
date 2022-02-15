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

import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';

@Component({
  selector: 'policy-studio-debug-inspector-table',
  template: require('./policy-studio-debug-inspector-table.component.html'),
  styles: [require('./policy-studio-debug-inspector-table.component.scss')],
})
export class PolicyStudioDebugInspectorTableComponent implements OnChanges {
  @Input()
  private name: string;

  @Input()
  private input: Record<string, unknown>;

  @Input()
  private output: Record<string, unknown>;

  private items: { key: string; outputValue: unknown; inputValue: unknown }[];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['input'] || changes['output']) {
      const input = this.input || {};
      const output = this.output || {};
      const keys = [...new Set(Object.keys(input).concat(Object.keys(output)))];

      this.items = keys.map((key) => ({ key, inputValue: input[key] || '', outputValue: output[key] || '' }));
    }
  }
}
