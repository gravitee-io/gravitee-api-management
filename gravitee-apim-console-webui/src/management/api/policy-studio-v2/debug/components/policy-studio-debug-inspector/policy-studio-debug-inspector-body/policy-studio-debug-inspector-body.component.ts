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
  selector: 'policy-studio-debug-inspector-body',
  templateUrl: './policy-studio-debug-inspector-body.component.html',
  styleUrls: ['./policy-studio-debug-inspector-body.component.scss'],
  standalone: false,
})
export class PolicyStudioDebugInspectorBodyComponent implements OnChanges {
  @Input()
  input: string;

  @Input()
  output: string;

  formattedInput: string;
  formattedOutput: string;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.input) {
      try {
        // FIXME: Remove this hack working only for JSON when we will have a proper diff for the body
        this.formattedInput = JSON.stringify(JSON.parse(changes.input.currentValue), null, 2);
      } catch (e) {
        this.formattedInput = changes.input.currentValue;
      }
    }

    if (changes.output) {
      try {
        // FIXME: Remove this hack working only for JSON when we will have a proper diff for the body
        this.formattedOutput = JSON.stringify(JSON.parse(changes.output.currentValue), null, 2);
      } catch (e) {
        this.formattedOutput = changes.output.currentValue;
      }
    }
  }
}
