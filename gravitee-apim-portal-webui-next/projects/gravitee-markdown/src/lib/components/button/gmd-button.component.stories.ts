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
import type { Meta, StoryObj } from '@storybook/angular';
import { importProvidersFrom } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { applicationConfig, moduleMetadata } from '@storybook/angular';
import { GmdButtonComponent } from './gmd-button.component';
import { GraviteeMarkdownEditorModule } from '../../gravitee-markdown-editor/gravitee-markdown-editor.module';

export default {
  title: 'Components/Button',
  component: GmdButtonComponent,
  parameters: {
    layout: 'centered',
  },
  argTypes: {
    appearance: {
      control: 'select',
      options: ['filled', 'outlined', 'text'],
    },
  },
  decorators: [
    moduleMetadata({
      imports: [GmdButtonComponent, GraviteeMarkdownEditorModule, ReactiveFormsModule],
    }),
    applicationConfig({
      providers: [importProvidersFrom(GraviteeMarkdownEditorModule.forRoot({ theme: 'vs', baseUrl: '.' }))],
    }),
  ],
} as Meta<GmdButtonComponent>;


export const DefaultAppearances: StoryObj<GmdButtonComponent> = {
  render: () => ({
    template: `
      <div style="display: flex; gap: 16px; align-items: center;">
        <gmd-button appearance="filled">Filled</gmd-button>
        <gmd-button appearance="outlined">Outlined</gmd-button>
        <gmd-button appearance="text">Text</gmd-button>
      </div>
    `,
  }),
};

export const WithCustomTheming: StoryObj<GmdButtonComponent> = {
  render: () => ({
    template: `
      <div style="
        /* Filled Button Tokens */
        --gmd-button-filled-label-text-font: 'Comic Sans MS', serif;
        --gmd-button-filled-label-text-weight: 600;
        --gmd-button-filled-label-text-size: 16px;
        --gmd-button-filled-label-text-tracking: 0.5px;
        --gmd-button-filled-label-text-transform: uppercase;
        --gmd-button-filled-label-text-color: #ffffff;
        --gmd-button-filled-container-color: #e91e63;
        --gmd-button-filled-container-shape: 99px;
        --gmd-button-filled-horizontal-padding: 24px;
        --gmd-button-filled-hover-container-color: #c2185b;
        --gmd-button-filled-active-container-color: #ad1457;
        --gmd-button-filled-active-outline-color: #f8bbd9;

        /* Outlined Button Tokens */
        --gmd-button-outlined-label-text-font: 'Comic Sans MS', serif;
        --gmd-button-outlined-label-text-weight: 600;
        --gmd-button-outlined-label-text-size: 16px;
        --gmd-button-outlined-label-text-tracking: 0.5px;
        --gmd-button-outlined-label-text-transform: uppercase;
        --gmd-button-outlined-label-text-color: #e91e63;
        --gmd-button-outlined-outline-width: 2px;
        --gmd-button-outlined-outline-color: #e91e63;
        --gmd-button-outlined-container-shape: 99px;
        --gmd-button-outlined-horizontal-padding: 24px;
        --gmd-button-outlined-hover-container-color: #fce4ec;
        --gmd-button-outlined-active-container-color: #f8bbd9;

        /* Text Button Tokens */
        --gmd-button-text-label-text-font: 'Comic Sans MS', serif;
        --gmd-button-text-label-text-weight: 600;
        --gmd-button-text-label-text-size: 16px;
        --gmd-button-text-label-text-tracking: 0.5px;
        --gmd-button-text-label-text-transform: uppercase;
        --gmd-button-text-label-text-color: #e91e63;
        --gmd-button-text-container-shape: 99px;
        --gmd-button-text-horizontal-padding: 24px;
        --gmd-button-text-hover-container-color: #fce4ec;
        --gmd-button-text-active-container-color: #f8bbd9;
      ">
        <div style="display: flex; flex-direction: column; gap: 24px; align-items: center;">
          <h3 style="margin: 0; color: #333;">Custom Theming Example</h3>
          <div style="display: flex; gap: 16px; align-items: center; flex-wrap: wrap; justify-content: center;">
            <gmd-button appearance="filled">Custom Filled</gmd-button>
            <gmd-button appearance="outlined">Custom Outlined</gmd-button>
            <gmd-button appearance="text">Custom Text</gmd-button>
          </div>
        </div>
      </div>
    `,
  }),
};

export const WithMarkdownEditor: StoryObj<GmdButtonComponent> = {
  render: () => ({
    template: `
      <div style="height: 700px; display: flex; flex-direction: column;">
        <h3 style="margin: 0 0 16px 0; color: #333;">Button Component in Markdown Editor</h3>

        <!-- Button Controls -->
        <div style="margin-bottom: 16px; display: flex; gap: 12px; align-items: center; flex-wrap: wrap;">
          <gmd-button appearance="filled">Save Document</gmd-button>
          <gmd-button appearance="outlined">Preview</gmd-button>
          <gmd-button appearance="text">Export</gmd-button>
          <gmd-button appearance="filled">Publish</gmd-button>
          <gmd-button appearance="outlined">Share</gmd-button>
          <gmd-button appearance="text">Settings</gmd-button>
        </div>

        <!-- Markdown Editor -->
        <div style="flex: 1; min-height: 400px;">
          <gmd-editor [formControl]="contentControl"></gmd-editor>
        </div>

        <!-- Status Bar -->
        <div style="margin-top: 12px; padding: 8px 12px; background-color: #f5f5f5; border-radius: 4px; font-size: 14px; color: #666;">
          <span>Characters: {{ contentControl.value?.length || 0 }} | Words: {{ getWordCount() }} | Lines: {{ getLineCount() }}</span>
        </div>
      </div>
    `,
    props: {
      contentControl: new FormControl(`# Button Component Integration

This markdown editor demonstrates the **button component** integration with the Gravitee Markdown Editor.

## Code Example

\`\`\`typescript
// Button component usage
<gmd-button appearance="filled">Save</gmd-button>
<gmd-button appearance="outlined">Preview</gmd-button>
<gmd-button appearance="text">Cancel</gmd-button>
\`\`\`

> All buttons use the token-based theming system for consistent styling across the application.

---

*Try editing this content and see how the buttons work alongside the markdown editor!*`),
      getWordCount: function() {
        const text = this['contentControl']?.value || '';
        return text.trim() ? text.trim().split(/\s+/).length : 0;
      },
      getLineCount: function() {
        const text = this['contentControl']?.value || '';
        return text ? text.split('\n').length : 0;
      }
    },
  }),
  parameters: {
    docs: {
      description: {
        story: 'This story demonstrates the button component integration with the Gravitee Markdown Editor, showing how buttons can be used as controls alongside the editor with real-time content statistics.',
      },
    },
  },
};
