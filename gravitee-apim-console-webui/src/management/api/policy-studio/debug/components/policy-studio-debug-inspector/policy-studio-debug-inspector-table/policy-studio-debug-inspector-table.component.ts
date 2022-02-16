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

import { getDiffState } from '../policy-studio-debug-inspector.component';

interface RowItem {
  key: string;
  outputValue: string;
  inputValue: string;
  diffClass: 'added' | 'deleted' | 'updated';
}

@Component({
  selector: 'policy-studio-debug-inspector-table',
  template: require('./policy-studio-debug-inspector-table.component.html'),
  styles: [require('./policy-studio-debug-inspector-table.component.scss')],
})
export class PolicyStudioDebugInspectorTableComponent implements OnChanges {
  @Input()
  name: string;

  @Input()
  input: Record<string, string | Array<string> | boolean>;

  @Input()
  output: Record<string, string | Array<string> | boolean>;

  rowItems: RowItem[];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['input'] || changes['output']) {
      this.rowItems = this.buildRowItems();
    }
  }

  buildRowItems(): RowItem[] {
    const input = this.input || {};
    const output = this.output || {};
    const keys = [...new Set(Object.keys(input).concat(Object.keys(output)))].sort((a, b) =>
      a.toLowerCase().localeCompare(b.toLowerCase()),
    );

    return keys.map((key) => {
      const inputValue = (input[key] || '').toString();
      const outputValue = (output[key] || '').toString();

      return { key, inputValue, outputValue, diffClass: getDiffState(inputValue, outputValue) };
    });
  }
}
