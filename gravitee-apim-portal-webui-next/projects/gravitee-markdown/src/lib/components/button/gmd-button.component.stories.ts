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
import { importProvidersFrom } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import type { Meta, StoryObj } from '@storybook/angular';
import { applicationConfig, moduleMetadata } from '@storybook/angular';

import { GmdButtonComponent } from './gmd-button.component';
import { GraviteeMarkdownEditorModule } from '../../gravitee-markdown-editor/gravitee-markdown-editor.module';
import { GridComponent } from '../grid/grid.component';

export default {
  title: 'Gravitee Markdown/Components/Button',
  component: GmdButtonComponent,
  parameters: {
    layout: 'centered',
  },
  argTypes: {
    appearance: {
      control: 'select',
      options: ['filled', 'outlined', 'text'],
    },
    link: {
      control: 'text',
    },
    target: {
      control: 'select',
      options: ['_self', '_blank'],
    },
  },
  decorators: [
    moduleMetadata({
      imports: [GmdButtonComponent, GraviteeMarkdownEditorModule, ReactiveFormsModule, GridComponent],
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
      <div class="with-custom-theming">
        <div style="display: flex; flex-direction: column; gap: 24px; align-items: center;">
          <h3 style="margin: 0; color: #333;">Custom Theming Example</h3>
          <div style="font-family: 'Comic Sans MS', sans-serif; text-transform: capitalize; display: flex; gap: 8px; align-items: center;">
            <gmd-button appearance="filled">Custom Filled</gmd-button>
            <gmd-button appearance="outlined">Custom Outlined</gmd-button>
            <gmd-button appearance="text">Custom Text</gmd-button>
          </div>
        </div>
      </div>
    `,
  }),
  parameters: {
    docs: {
      description: {
        story:
          'Demonstrates custom theming using the actual @gmd.button-overrides() SCSS mixin. The styling is applied via the .custom-themed-buttons class which uses the mixin internally, showing how it would work in a real application.',
      },
    },
  },
};

export const WithInternalLinks: StoryObj<GmdButtonComponent> = {
  render: () => ({
    template: `
      <div style="display: flex; flex-direction: column; gap: 16px; align-items: center;">
        <h3 style="margin: 0; color: #333;">Internal Links (Same Tab)</h3>
        <div style="display: flex; gap: 16px; align-items: center; flex-wrap: wrap; justify-content: center;">
          <gmd-button appearance="filled" link="/dashboard">Dashboard</gmd-button>
          <gmd-button appearance="outlined" link="/settings">Settings</gmd-button>
          <gmd-button appearance="text" link="/profile">Profile</gmd-button>
        </div>
        <p style="margin: 0; color: #666; font-size: 14px; text-align: center;">
          Internal links start with "/" and open in the same tab by default
        </p>
      </div>
    `,
  }),
  parameters: {
    docs: {
      description: {
        story: 'Demonstrates internal navigation buttons that link to different pages within the application.',
      },
    },
  },
};

export const WithExternalLinks: StoryObj<GmdButtonComponent> = {
  render: () => ({
    template: `
      <div style="display: flex; flex-direction: column; gap: 16px; align-items: center;">
        <h3 style="margin: 0; color: #333;">External Links (New Tab)</h3>
        <div style="display: flex; gap: 16px; align-items: center; flex-wrap: wrap; justify-content: center;">
          <gmd-button appearance="filled" link="https://gravitee.io" target="_blank">Gravitee.io</gmd-button>
          <gmd-button appearance="outlined" link="https://docs.gravitee.io" target="_blank">Documentation</gmd-button>
          <gmd-button appearance="text" link="https://github.com/gravitee-io" target="_blank">GitHub</gmd-button>
        </div>
        <p style="margin: 0; color: #666; font-size: 14px; text-align: center;">
          External links open in a new tab with target="_blank"
        </p>
      </div>
    `,
  }),
  parameters: {
    docs: {
      description: {
        story: 'Demonstrates external navigation buttons that link to external websites and open in new tabs.',
      },
    },
  },
};

export const WithMarkdownEditor: StoryObj<GmdButtonComponent> = {
  render: () => ({
    template: `
      <div style="height: 700px; display: flex; flex-direction: column;">
        <h3 style="margin: 0 0 16px 0; color: #333;">Button Component in Markdown Editor</h3>

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

## Button component usage

<gmd-grid columns="3">
  <gmd-cell style="text-align: center;">
    <gmd-button appearance="filled" link="/save">Save</gmd-button>
  </gmd-cell>
  <gmd-cell style="text-align: center;">
    <gmd-button appearance="outlined" link="/preview">Preview</gmd-button>
  </gmd-cell>
  <gmd-cell style="text-align: center;">
    <gmd-button appearance="text" link="/cancel">Cancel</gmd-button>
  </gmd-cell>
</gmd-grid>

## External Links

<gmd-grid columns="2">
  <gmd-cell style="text-align: center;">
    <gmd-button appearance="filled" link="https://gravitee.io" target="_blank">Visit Gravitee</gmd-button>
  </gmd-cell>
  <gmd-cell style="text-align: center;">
    <gmd-button appearance="outlined" link="https://docs.gravitee.io" target="_blank">Read Docs</gmd-button>
  </gmd-cell>
</gmd-grid>

> All buttons use the token-based theming system for consistent styling across the application.

---

*Try editing this content and see how the buttons work alongside the markdown editor!*`),
      getWordCount: function () {
        const text = this['contentControl']?.value || '';
        return text.trim() ? text.trim().split(/\s+/).length : 0;
      },
      getLineCount: function () {
        const text = this['contentControl']?.value || '';
        return text ? text.split('\n').length : 0;
      },
    },
  }),
  parameters: {
    docs: {
      description: {
        story:
          'This story demonstrates the button component integration with the Gravitee Markdown Editor, showing how buttons can be used as controls alongside the editor with real-time content statistics.',
      },
    },
  },
};
