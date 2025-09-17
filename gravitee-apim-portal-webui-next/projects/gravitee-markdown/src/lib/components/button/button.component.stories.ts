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
import { ButtonComponent } from './button.component';

export default {
  title: 'Components/Button',
  component: ButtonComponent,
  parameters: {
    layout: 'centered',
  },
  argTypes: {
    appearance: {
      control: 'select',
      options: ['filled', 'outlined', 'text'],
    },
  },
} as Meta<ButtonComponent>;

type Story = StoryObj<ButtonComponent>;

export const Filled: Story = {
  args: {
    appearance: 'filled',
  },
  render: (args) => ({
    props: args,
    template: `<gmd-button [appearance]="appearance">Filled Button</gmd-button>`,
  }),
};

export const Outlined: Story = {
  args: {
    appearance: 'outlined',
  },
  render: (args) => ({
    props: args,
    template: `<gmd-button [appearance]="appearance">Outlined Button</gmd-button>`,
  }),
};

export const Text: Story = {
  args: {
    appearance: 'text',
  },
  render: (args) => ({
    props: args,
    template: `<gmd-button [appearance]="appearance">Text Button</gmd-button>`,
  }),
};

export const AllAppearances: StoryObj<ButtonComponent> = {
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

export const WithCustomTheming: StoryObj<ButtonComponent> = {
  render: () => ({
    template: `
      <div style="--gmd-button-filled-container-color: #e91e63; --gmd-button-filled-text-color: #ffffff; --gmd-button-outlined-outline-color: #e91e63; --gmd-button-outlined-text-color: #e91e63; --gmd-button-text-text-color: #e91e63;">
        <div style="display: flex; gap: 16px; align-items: center; margin-bottom: 16px;">
          <gmd-button appearance="filled">Custom Filled</gmd-button>
          <gmd-button appearance="outlined">Custom Outlined</gmd-button>
          <gmd-button appearance="text">Custom Text</gmd-button>
        </div>
      </div>
    `,
  }),
};