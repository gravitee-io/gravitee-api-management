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
import { CommonModule } from '@angular/common';
import { NgModule, ModuleWithProviders } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { MonacoEditorComponent } from './components/monaco-editor/monaco-editor.component';
import { GraviteeMarkdownEditorComponent } from './gravitee-markdown-editor.component';
import { GmdMonacoEditorConfig } from './models/monaco-editor-config';
import { GMD_CONFIG } from './tokens/gmd-config.token';
import { GraviteeMarkdownViewerModule } from '../gravitee-markdown-viewer/gravitee-markdown-viewer.module';

@NgModule({
  imports: [CommonModule, FormsModule, ReactiveFormsModule, GraviteeMarkdownViewerModule],
  exports: [GraviteeMarkdownEditorComponent, MonacoEditorComponent],
  declarations: [MonacoEditorComponent, GraviteeMarkdownEditorComponent],
  providers: [
    {
      provide: GMD_CONFIG,
      useValue: {} as GmdMonacoEditorConfig,
    },
  ],
})
export class GraviteeMarkdownEditorModule {
  static forRoot(config?: GmdMonacoEditorConfig): ModuleWithProviders<GraviteeMarkdownEditorModule> {
    return {
      ngModule: GraviteeMarkdownEditorModule,
      providers: [
        {
          provide: GMD_CONFIG,
          useValue: config || {},
        },
      ],
    };
  }
}
