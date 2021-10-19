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
import { Meta, moduleMetadata } from '@storybook/angular';
import { Story } from '@storybook/angular/dist/ts3.9/client/preview/types-7-0';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconModule } from '@angular/material/icon';

import { GioFormSlideToggleComponent } from './gio-form-slide-toggle.component';
import { GioFormSlideToggleModule } from './gio-form-slide-toggle.module';

export default {
  title: 'Shared / Form slide toggle',
  component: GioFormSlideToggleComponent,
  decorators: [
    moduleMetadata({
      imports: [BrowserAnimationsModule, GioFormSlideToggleModule, MatSlideToggleModule, MatFormFieldModule, MatInputModule, MatIconModule],
    }),
  ],
  render: () => ({}),
} as Meta;

export const OnlyToggle: Story = {
  render: () => ({
    template: '<gio-form-slide-toggle><mat-slide-toggle gioFormSlideToggle></mat-slide-toggle></gio-form-slide-toggle>',
  }),
};

export const WithLabel1: Story = {
  render: () => ({
    template: `
      <gio-form-slide-toggle>
        <gio-form-label>Label 1</gio-form-label>
        <mat-slide-toggle gioFormSlideToggle></mat-slide-toggle>
      </gio-form-slide-toggle>
    `,
  }),
};

export const WithLabel2: Story = {
  render: () => ({
    template: `
      <gio-form-slide-toggle>
        Label 2 - Description
        <mat-slide-toggle gioFormSlideToggle></mat-slide-toggle>
      </gio-form-slide-toggle>
    `,
  }),
};

export const WithLabel1AndLabel2: Story = {
  render: () => ({
    template: `
      <gio-form-slide-toggle>
        <gio-form-label>Label 1</gio-form-label>
        Label 2 - Description
        <mat-slide-toggle gioFormSlideToggle></mat-slide-toggle>
      </gio-form-slide-toggle>
    `,
  }),
};

export const SimilarToMatFormField: Story = {
  render: ({ appearance }) => ({
    template: `
      <p>
        <mat-form-field [appearance]="appearance">
          <mat-label>Standard form field</mat-label>
          <input matInput placeholder="Placeholder">
          <mat-hint>Hint</mat-hint>
        </mat-form-field>
      </p>
      <p>
        <gio-form-slide-toggle [appearance]="appearance">
          <gio-form-label>Label 1</gio-form-label>
          Label 2 - Description
          <mat-slide-toggle gioFormSlideToggle></mat-slide-toggle>
        </gio-form-slide-toggle>
      </p>
    `,
    props: { appearance },
  }),
  argTypes: {
    appearance: {
      options: ['standard', 'fill'],
      control: { type: 'select' },
    },
  },
  args: {
    appearance: 'fill',
  },
};

export const SimilarToMatFormFieldWithIcon: Story = {
  render: ({ appearance }) => ({
    template: `
      <p class="mat-body">
        <mat-form-field [appearance]="appearance">
          <mat-icon matPrefix>lock</mat-icon>
          <mat-label>Standard form field</mat-label>
          <input matInput placeholder="Placeholder">
          <mat-hint>Hint</mat-hint>
        </mat-form-field>
      </p>
      <p class="mat-body">
        <gio-form-slide-toggle [appearance]="appearance">
          <mat-icon gioFormPrefix>lock</mat-icon>
          <gio-form-label>Label 1</gio-form-label>
          Label 2 - Description
          <mat-slide-toggle gioFormSlideToggle></mat-slide-toggle>
        </gio-form-slide-toggle>
      </p>
    `,
    props: { appearance },
  }),
  argTypes: {
    appearance: {
      options: ['standard', 'fill'],
      control: { type: 'select' },
    },
  },
  args: {
    appearance: 'standard',
  },
};

export const SimilarToMatFormFieldDisabled: Story = {
  render: ({ disabled, appearance }) => ({
    template: `
      <p>
        <mat-form-field [appearance]="appearance">
          <mat-icon matPrefix>lock</mat-icon>
          <mat-label>Standard form field</mat-label>
          <input [disabled]="disabled" matInput value="Value">
          <mat-hint>Hint</mat-hint>
        </mat-form-field>
      </p>
      <p>
        <gio-form-slide-toggle [appearance]="appearance">
          <mat-icon gioFormPrefix>lock</mat-icon>
          <gio-form-label>Label 1</gio-form-label>
          Label 2 - Description
          <mat-slide-toggle gioFormSlideToggle [disabled]="disabled"></mat-slide-toggle>
        </gio-form-slide-toggle>
      </p>
    `,
    props: { disabled, appearance },
  }),
  argTypes: {
    disabled: {
      type: { name: 'boolean', required: false },
    },
    appearance: {
      options: ['standard', 'fill'],
      control: { type: 'select' },
    },
  },
  args: {
    disabled: true,
    appearance: 'fill',
  },
};

export const FullWidth: Story = {
  render: ({ disabled, appearance }) => ({
    template: `
      <p>
        <mat-form-field [appearance]="appearance" style="width:100%">
          <mat-icon matPrefix>lock</mat-icon>
          <mat-label>Standard form field</mat-label>
          <input [disabled]="disabled" matInput value="Value">
          <mat-hint>Hint</mat-hint>
        </mat-form-field>
      </p>
      <p>
        <gio-form-slide-toggle [appearance]="appearance" style="width:100%">
          <mat-icon gioFormPrefix>lock</mat-icon>
          <gio-form-label>Label 1</gio-form-label>
          Label 2 - Description
          <mat-slide-toggle gioFormSlideToggle [disabled]="disabled"></mat-slide-toggle>
        </gio-form-slide-toggle>
      </p>
    `,
    props: { disabled, appearance },
  }),
  argTypes: {
    disabled: {
      type: { name: 'boolean', required: false },
    },
    appearance: {
      options: ['standard', 'fill'],
      control: { type: 'select' },
    },
  },
  args: {
    disabled: false,
    appearance: 'fill',
  },
};
