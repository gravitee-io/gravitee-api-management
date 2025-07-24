/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { Component, Input, Output, EventEmitter, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GraviteeMarkdownViewerComponent } from '../gravitee-markdown-viewer/gravitee-markdown-viewer.component';
import { GraviteeMonacoWrapperModule } from '../gravitee-monaco-wrapper/gravitee-monaco-wrapper.module';

@Component({
  selector: 'gravitee-markdown-editor',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    GraviteeMarkdownViewerComponent,
    GraviteeMonacoWrapperModule
  ],
  templateUrl: './gravitee-markdown-editor.component.html',
  styleUrls: ['./gravitee-markdown-editor.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class GraviteeMarkdownEditorComponent {

  @Input() darkTheme: boolean = false;
  @Input() highlightTheme: string = 'github';
  @Output() contentChange = new EventEmitter<string>();
  @Output() errorChange = new EventEmitter<string | null>();

  content: string = '';
  error: string | null = null;
}
