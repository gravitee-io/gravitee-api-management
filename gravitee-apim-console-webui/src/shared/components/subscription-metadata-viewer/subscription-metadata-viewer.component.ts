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
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { GioMonacoEditorModule, MonacoEditorLanguageConfig } from '@gravitee/ui-particles-angular';
import { FormsModule } from '@angular/forms';
import { editor } from 'monaco-editor';

@Component({
  selector: 'subscription-metadata-viewer',
  templateUrl: './subscription-metadata-viewer.component.html',
  styleUrls: ['./subscription-metadata-viewer.component.scss'],
  standalone: true,
  imports: [GioMonacoEditorModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SubscriptionMetadataViewerComponent {
  metadata = input<Record<string, string> | null | undefined>();

  protected metadataJson = computed(() => {
    const m = this.metadata();
    return !m || Object.keys(m).length === 0 ? '' : JSON.stringify(m, null, 2);
  });

  protected readonly monacoEditorOptions: editor.IStandaloneEditorConstructionOptions = {
    lineNumbers: 'off',
    renderLineHighlight: 'none',
    hideCursorInOverviewRuler: true,
    overviewRulerBorder: false,
    readOnly: true,
    wordWrap: 'on',
    scrollBeyondLastLine: false,
    padding: { top: 0, bottom: 0 },
    scrollbar: {
      vertical: 'auto',
      horizontal: 'hidden',
      useShadows: false,
    },
  };

  protected readonly languageConfig: MonacoEditorLanguageConfig = { language: 'json', schemas: [] };
}
