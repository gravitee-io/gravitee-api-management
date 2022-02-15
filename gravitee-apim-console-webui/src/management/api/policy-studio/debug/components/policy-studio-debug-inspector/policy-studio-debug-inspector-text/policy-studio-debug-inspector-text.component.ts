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

@Component({
  selector: 'policy-studio-debug-inspector-text',
  template: require('./policy-studio-debug-inspector-text.component.html'),
  styles: [require('./policy-studio-debug-inspector-text.component.scss')],
})
export class PolicyStudioDebugInspectorTextComponent implements OnChanges {
  @Input()
  name: string;

  @Input()
  input: string;

  @Input()
  output: string;

  diffClass: 'added' | 'deleted' | 'updated';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['input'] || changes['output']) {
      this.diffClass = getDiffState(this.input, this.output);
    }
  }
}
