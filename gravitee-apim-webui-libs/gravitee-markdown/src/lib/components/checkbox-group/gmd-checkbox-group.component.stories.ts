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
import { FormsModule } from '@angular/forms';
import { Meta, moduleMetadata, StoryObj, applicationConfig } from '@storybook/angular';

import { GmdCheckboxGroupComponent } from './gmd-checkbox-group.component';
import { GraviteeMarkdownEditorModule } from '../../gravitee-markdown-editor/gravitee-markdown-editor.module';
import { GmdFormEditorComponent } from '../../gravitee-markdown-form-editor/gmd-form-editor.component';
import { GraviteeMarkdownViewerModule } from '../../gravitee-markdown-viewer/gravitee-markdown-viewer.module';
import { provideGmdFormStore } from '../../services/gmd-form-state.store';

export default {
  title: 'Gravitee Markdown/Components/Checkbox Group',
  component: GmdCheckboxGroupComponent,
  parameters: {
    layout: 'centered',
  },
  decorators: [
    moduleMetadata({
      imports: [GmdCheckboxGroupComponent, GmdFormEditorComponent, GraviteeMarkdownEditorModule, GraviteeMarkdownViewerModule, FormsModule],
    }),
    applicationConfig({
      providers: [importProvidersFrom(GraviteeMarkdownEditorModule.forRoot({ theme: 'vs', baseUrl: '.' })), provideGmdFormStore()],
    }),
  ],
} as Meta<GmdCheckboxGroupComponent>;

export const Basic: StoryObj<GmdCheckboxGroupComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px;">
        <gmd-checkbox-group name="basic" label="Basic Checkbox Group" options="Option 1,Option 2,Option 3"></gmd-checkbox-group>
      </div>
    `,
  }),
};

export const Required: StoryObj<GmdCheckboxGroupComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px;">
        <gmd-checkbox-group name="required" label="Required Checkbox Group" required="true" options="Option 1,Option 2,Option 3"></gmd-checkbox-group>
      </div>
    `,
  }),
};

export const Disabled: StoryObj<GmdCheckboxGroupComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px;">
        <gmd-checkbox-group name="disabled" label="Disabled Checkbox Group" disabled="true" value="Option 1,Option 3" options="Option 1,Option 2,Option 3"></gmd-checkbox-group>
      </div>
    `,
  }),
};

export const PreSelectedValues: StoryObj<GmdCheckboxGroupComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px;">
        <gmd-checkbox-group
          name="preselected"
          label="Pre-selected Options"
          value="Option 1,Option 3"
          options="Option 1,Option 2,Option 3,Option 4">
        </gmd-checkbox-group>
      </div>
    `,
  }),
  parameters: {
    docs: {
      description: {
        story: 'Checkbox group with pre-selected values provided as comma-separated string.',
      },
    },
  },
};

export const WithDynamicOptionsFallback: StoryObj<GmdCheckboxGroupComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px;">
        <gmd-checkbox-group
          name="dynamic"
          label="Enabled capabilities (EL fallback preview)"
          options="{#api.metadata['capabilities']}:Analytics,Rate Limiting,Caching">
        </gmd-checkbox-group>
      </div>
    `,
  }),
  parameters: {
    docs: {
      description: {
        story:
          'Example of EL options with fallback. In preview contexts, fallback values are displayed. Runtime-resolved options are injected separately.',
      },
    },
  },
};

export const ManyOptions: StoryObj<GmdCheckboxGroupComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px;">
        <gmd-checkbox-group
          name="many"
          label="Features"
          required="true"
          options="Authentication,Authorization,Rate Limiting,Analytics,Caching,Logging,Transformation,Monitoring,Alerting,Load Balancing">
        </gmd-checkbox-group>
      </div>
    `,
  }),
};

export const WithFieldKey: StoryObj<GmdCheckboxGroupComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px; display: flex; flex-direction: column; gap: 24px;">
        <gmd-checkbox-group
          name="features"
          label="Required Features"
          fieldKey="requiredFeatures"
          required="true"
          options="Authentication,Rate Limiting,Analytics,Caching">
        </gmd-checkbox-group>
        <gmd-checkbox-group
          name="environments"
          label="Target Environments"
          fieldKey="targetEnvironments"
          options="Development,Staging,Production">
        </gmd-checkbox-group>
      </div>
    `,
  }),
  parameters: {
    docs: {
      description: {
        story: 'Checkbox groups configured with field keys for validation and data collection.',
      },
    },
  },
};

export const WithFormEditor: StoryObj = {
  render: () => ({
    template: `
      <style>
        .form-editor-container {
          height: 600px;
        }
      </style>
      <div style="padding: 20px; display: flex; flex-direction: column; gap: 16px;">
        <h2>Checkbox Group with Form Editor</h2>
        <p>Edit the markdown content below. The form editor shows real-time validation status and field values:</p>

        <div class="form-editor-container">
          <gmd-form-editor [(ngModel)]="formContent" />
        </div>
      </div>
    `,
    props: {
      formContent: `# API Subscription Configuration

Select the features and environments for your subscription.

## Required Features

<gmd-checkbox-group
  name="features"
  label="Required Features"
  fieldKey="features"
  required="true"
  options="Authentication,Rate Limiting,Analytics,Caching,Logging">
</gmd-checkbox-group>

## Target Environments

<gmd-checkbox-group
  name="environments"
  label="Target Environments"
  fieldKey="environments"
  options="Development,Staging,Production">
</gmd-checkbox-group>`,
    },
  }),
  parameters: {
    docs: {
      description: {
        story: 'Form editor with live validation tracking for checkbox groups.',
      },
    },
  },
};

export const InMarkdownViewer: StoryObj = {
  render: () => ({
    template: `
      <div style="width: 600px;">
        <gmd-viewer [content]="content"></gmd-viewer>
      </div>
    `,
    props: {
      content: `# Checkbox Group Examples

<gmd-checkbox-group name="features" label="Features" required="true" options="Authentication,Rate Limiting,Analytics"></gmd-checkbox-group>

<gmd-checkbox-group name="envs" label="Environments" options="Development,Staging,Production"></gmd-checkbox-group>
`,
    },
  }),
  parameters: {
    docs: {
      description: {
        story: 'Checkbox groups rendered within the GMD viewer, showing how they appear in actual markdown content.',
      },
    },
  },
};
