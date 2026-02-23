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
import { ComponentHarness } from '@angular/cdk/testing';
import { TestBed } from '@angular/core/testing';

import { GmdFormEditorComponent } from './gmd-form-editor.component';
import { MockMonacoEditorStandaloneComponent } from './gmd-form-editor.test.component';
import { GraviteeMarkdownEditorModule } from '../gravitee-markdown-editor/gravitee-markdown-editor.module';

export const ConfigureTestingGmdFormEditor = () => {
  try {
    TestBed.overrideComponent(GmdFormEditorComponent, {
      remove: {
        imports: [GraviteeMarkdownEditorModule],
      },
      add: {
        imports: [MockMonacoEditorStandaloneComponent],
      },
    });
  } catch (e) {
    // Do nothing
  }
};

ConfigureTestingGmdFormEditor();

export class GmdFormEditorHarness extends ComponentHarness {
  static readonly hostSelector = 'gmd-form-editor';

  private readonly getEditorInput = this.locatorFor('gmd-monaco-editor input');

  async getEditorValue(): Promise<string> {
    const input = await this.getEditorInput();
    return input.getProperty('value');
  }

  async isEditorReadOnly(): Promise<boolean> {
    const input = await this.getEditorInput();
    const disabled = await input.getAttribute('disabled');
    return disabled !== null;
  }

  async getStatusLabelText(): Promise<string | null> {
    try {
      const label = await this.locatorFor('.form-status__label')();
      return (await label.text()).trim();
    } catch {
      return null;
    }
  }

  async getStatusBadgeText(): Promise<string | null> {
    try {
      const badge = await this.locatorFor('.form-status__badge')();
      return (await badge.text()).trim();
    } catch {
      return null;
    }
  }
}
