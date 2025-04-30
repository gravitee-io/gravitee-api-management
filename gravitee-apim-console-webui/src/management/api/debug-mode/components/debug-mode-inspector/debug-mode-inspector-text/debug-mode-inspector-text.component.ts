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

import { getDiffState } from '../debug-mode-inspector.component';

@Component({
  selector: 'debug-mode-inspector-text',
  templateUrl: './debug-mode-inspector-text.component.html',
  styleUrls: ['./debug-mode-inspector-text.component.scss'],
  standalone: false,
})
export class DebugModeInspectorTextComponent implements OnChanges {
  @Input()
  name: string;

  @Input()
  input: string;

  @Input()
  output: string;

  @Input()
  noDiff = false;

  diffClass: 'added' | 'deleted' | 'updated';

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.noDiff && (changes['input'] || changes['output'])) {
      this.diffClass = getDiffState(this.input, this.output);
    }
  }
}
