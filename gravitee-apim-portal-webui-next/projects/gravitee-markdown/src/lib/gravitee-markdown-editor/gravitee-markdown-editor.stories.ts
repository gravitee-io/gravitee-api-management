/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { applicationConfig, Args, Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { GraviteeMarkdownEditorComponent } from './gravitee-markdown-editor.component';
import { GraviteeMarkdownEditorModule } from './gravitee-markdown-editor.module';

export default {
  title: 'Gravitee Markdown/Gravitee Markdown Editor',
  component: GraviteeMarkdownEditorComponent,
  decorators: [
    moduleMetadata({
      imports: [GraviteeMarkdownEditorModule, ReactiveFormsModule],
    }),
    applicationConfig({
      providers: [importProvidersFrom(GraviteeMarkdownEditorModule.forRoot({ theme: 'vs', baseUrl: '.' }))],
    }),
  ],
  parameters: {
    docs: {
      description: {
        component: 'A markdown editor component built with Monaco Editor that implements ControlValueAccessor for form integration.',
      },
    },
  },
} as Meta<GraviteeMarkdownEditorComponent>;

export const WithEmptyContent: StoryObj<GraviteeMarkdownEditorComponent> = {
  render: () => ({
    template: `
      <div style="height: 650px">
        <h3>Markdown Editor with Sample Content</h3>
        <gmd-editor [formControl]="contentControl"></gmd-editor>
        <div style="margin-top: 10px;">
          <p>Current value length: {{ contentControl.value?.length || 0 }} characters</p>
        </div>
      </div>
    `,
    props: {
      contentControl: new FormControl(''),
    },
  }),
};
export const WithSampleContent: StoryObj<GraviteeMarkdownEditorComponent> = {
  render: () => ({
    template: `
      <div style="height: 650px">
        <h3>Markdown Editor with Sample Content</h3>
        <gmd-editor [formControl]="contentControl"></gmd-editor>
        <div style="margin-top: 10px;">
          <p>Current value length: {{ contentControl.value?.length || 0 }} characters</p>
        </div>
      </div>
    `,
    props: {
      contentControl: new FormControl(`# Welcome to Gravitee API Management

This is a **markdown editor** component that allows you to write and edit markdown content.

## Features

- *Italic text* and **bold text**
- \`Inline code\`
- [Links](https://gravitee.io)
- Lists:
  - Item 1
  - Item 2
  - Item 3

## Code Block

\`\`\`javascript
function hello() {
  console.log("Hello, Gravitee!");
}
\`\`\`

> This is a blockquote

---

*Built with Monaco Editor for the best editing experience.*`),
    },
  }),
};
export const WithDisabledContent: StoryObj<GraviteeMarkdownEditorComponent> = {
  render: () => ({
    template: `
      <div style="height: 650px">
        <h3>Markdown Editor with Sample Content</h3>
        <gmd-editor [formControl]="contentControl"></gmd-editor>
        <div style="margin-top: 10px;">
          <p>Current value length: {{ contentControl.value?.length || 0 }} characters</p>
        </div>
      </div>
    `,
    props: {
      contentControl: new FormControl<string>({
        value: `# Disabled Editor

This editor is currently **disabled** and in read-only mode.

You cannot edit this content when the form control is disabled.`,
        disabled: true,
      }),
    },
  }),
};
export const WithFormControl: StoryObj<GraviteeMarkdownEditorComponent> = {
  render: () => ({
    template: `
      <div style="height: 650px">
        <h3>Markdown Editor with Validation</h3>
        <form [formGroup]="form">
          <gmd-editor formControlName="content"></gmd-editor>
          <div style="margin-top: 10px;">
            <p>Form valid: {{ form.valid }}</p>
            <p>Content errors: {{ form.get('content')?.errors | json }}</p>
            <button (click)="updateValue()">Update Value</button>
            <button (click)="clearValue()">Clear Value</button>
            <button (click)="toggleDisabled()">Toggle Disabled</button>
          </div>
        </form>
      </div>
    `,
    props: {
      form: new FormGroup({
        content: new FormControl('# Hello', [Validators.required, Validators.minLength(10)]),
      }),
      updateValue: function () {
        this['form'].get('content')?.setValue('This is a valid content that meets the minimum length requirement.');
      },
      clearValue: function () {
        this['form'].get('content')?.setValue('');
      },
      toggleDisabled: function () {
        if (this['form'].disabled) {
          this['form'].enable();
        } else {
          this[`form`].disable();
        }
      },
    },
  }),
};

export const ThemeVariants: StoryObj = {
  argTypes: {
    borderColor: {
      control: { type: 'color' },
      description: 'The color of the editor border',
    },
  },
  args: {
    borderColor: '#b2aaa9',
  },
  render: (args: Args) => ({
    template: `
      <div>
        <h3>Border Color Test</h3>
        <p>Use the color control below to change the border color dynamically.</p>

        <div style="height: 400px; --gmd-editor-container-outline-color: {{ borderColor }};">
          <gmd-editor [formControl]="contentControl"></gmd-editor>
        </div>
      </div>
    `,
    props: {
      contentControl: new FormControl(`# Border Color Test

This editor allows you to test different border colors dynamically.

## Features
- **Dynamic border color** - Change the color using the control panel
- **Real-time updates** - See changes immediately
- **Token system** - Uses CSS custom properties for theming

## Sample Content
- *Italic text* and **bold text**
- \`Inline code\`
- [Links](https://gravitee.io)

> This demonstrates the token-based theming system`),
      borderColor: args['borderColor'] || '#b2aaa9',
    },
  }),
  parameters: {
    docs: {
      description: {
        story:
          'This story allows you to dynamically change the border color using a color picker control. The border color is controlled by the CSS custom property `--gmd-editor-container-outline-color`.',
      },
    },
  },
};
