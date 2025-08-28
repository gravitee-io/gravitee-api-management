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
import { Component, input, InputSignal } from '@angular/core';
import { editor } from 'monaco-editor';
import { GioMonacoEditorModule } from '@gravitee/ui-particles-angular';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'api-proxy-request-log-body',
  templateUrl: './api-proxy-request-log-body.component.html',
  styleUrls: ['./api-proxy-request-log-body.component.scss'],
  imports: [GioMonacoEditorModule, FormsModule],
})
export class ApiProxyRequestLogBodyComponent {
  body: InputSignal<string> = input.required<string>();
  monacoEditorOptions: editor.IStandaloneEditorConstructionOptions = {
    renderLineHighlight: 'none',
    hideCursorInOverviewRuler: true,
    overviewRulerBorder: false,
    scrollbar: {
      vertical: 'hidden',
      horizontal: 'hidden',
      useShadows: false,
    },
  };
}
