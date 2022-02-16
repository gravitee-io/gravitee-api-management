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

import '@gravitee/ui-components/wc/gv-code';

@Component({
  selector: 'policy-studio-debug-inspector-body',
  template: require('./policy-studio-debug-inspector-body.component.html'),
  styles: [require('./policy-studio-debug-inspector-body.component.scss')],
})
export class PolicyStudioDebugInspectorBodyComponent implements OnChanges {
  @Input()
  input: string;

  @Input()
  output: string;

  formattedInput: string;
  formattedOutput: string;

  inputFormatOptions = {
    lineWrapping: true,
    lineNumbers: true,
    mode: {},
  };

  outputFormatOptions = {
    lineWrapping: true,
    lineNumbers: true,
    mode: {},
  };

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.input) {
      try {
        // FIXME: Remove this hack working only for JSON when we will have a proper diff for the body
        this.formattedInput = JSON.stringify(JSON.parse(changes.input.currentValue), null, 2);
        this.inputFormatOptions.mode = { name: 'javascript', json: true };
      } catch (e) {
        this.formattedInput = changes.input.currentValue;
      }
    }

    if (changes.output) {
      try {
        // FIXME: Remove this hack working only for JSON when we will have a proper diff for the body
        this.formattedOutput = JSON.stringify(JSON.parse(changes.output.currentValue), null, 2);
        this.outputFormatOptions.mode = { name: 'javascript', json: true };
      } catch (e) {
        this.formattedOutput = changes.output.currentValue;
      }
    }
  }
}
