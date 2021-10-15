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
import { action } from '@storybook/addon-actions';
import { MatFormFieldModule } from '@angular/material/form-field';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { GioFormTagsInputComponent } from './gio-form-tags-input.component';
import { GioFormTagsInputModule } from './gio-form-tags-input.module';

export default {
  title: 'Shared / Form tags input',
  component: GioFormTagsInputComponent,
  decorators: [
    moduleMetadata({
      imports: [BrowserAnimationsModule, GioFormTagsInputModule, FormsModule, ReactiveFormsModule, MatFormFieldModule],
    }),
  ],
  render: () => ({}),
  argTypes: {
    tags: {
      control: { type: 'array' },
      description: '',
      table: { type: { summary: 'string[]' }, defaultValue: [] },
    },
    placeholder: {
      control: { type: 'string' },
    },
    required: {
      defaultValue: false,
      control: { type: 'boolean' },
    },
    disabled: {
      defaultValue: false,
      control: { type: 'boolean' },
    },
  },
} as Meta;

export const WithoutFormField: Story = {
  render: ({ tags, placeholder, required, disabled }) => ({
    template: `
      <gio-form-tags-input [disabled]="disabled" [required]="required" [placeholder]="placeholder" [ngModel]="tags" (ngModelChange)="onTagsChange($event)">
      </gio-form-tags-input>
    `,
    props: {
      tags,
      placeholder,
      required,
      disabled,
      onTagsChange: (e) => action('Tags')(e),
    },
  }),
  args: {},
};

export const NgModelEmpty: Story = {
  render: ({ tags, placeholder, required, disabled }) => ({
    template: `
    <mat-form-field appearance="fill" style="width:100%">
      <mat-label>My tags</mat-label>
      <gio-form-tags-input 
        [disabled]="disabled" 
        [required]="required" 
        [placeholder]="placeholder" 
        [ngModel]="tags" 
        (ngModelChange)="onTagsChange($event)"
      >
      </gio-form-tags-input>
    </mat-form-field>
    `,
    props: {
      tags,
      placeholder,
      required,
      disabled,
      onTagsChange: (e) => action('Tags')(e),
    },
  }),
  args: {},
};

export const NgModelInitialValue: Story = {
  render: NgModelEmpty.render,
  args: {
    tags: ['A', 'B'],
    required: true,
    placeholder: 'Add a tag',
  },
};

export const NgModelRequired: Story = {
  render: NgModelEmpty.render,
  args: {
    tags: ['A', 'B'],
    required: true,
    placeholder: 'Add a tag',
  },
};

export const NgModelDisabled: Story = {
  render: NgModelEmpty.render,
  args: {
    tags: ['A', 'B'],
    required: true,
    disabled: true,
    placeholder: 'Add a tag',
  },
};

export const FormControlEmpty: Story = {
  render: ({ tags, placeholder, required, disabled, tagValidationHook }) => {
    const tagsControl = new FormControl({ value: tags, disabled });

    tagsControl.valueChanges.subscribe((value) => {
      action('Tags')(value);
    });

    return {
      template: `
      <mat-form-field appearance="fill" style="width:100%">
        <mat-label>My tags</mat-label>
        <gio-form-tags-input 
          [required]="required" 
          [placeholder]="placeholder" 
          [formControl]="tagsControl"
          [tagValidationHook]="tagValidationHook"
        >
        </gio-form-tags-input>
      </mat-form-field>
      `,
      props: {
        tags,
        placeholder,
        required,
        disabled,
        tagsControl,
        tagValidationHook,
      },
    };
  },
  args: {},
};

export const WithTagValidationHook: Story = {
  render: FormControlEmpty.render,
  args: {
    tags: ['A'],
    required: true,
    disabled: true,
    placeholder: 'Add a tag',
    tagValidationHook: (tag: string, validationCb: (shouldAddTag: boolean) => void) => {
      validationCb(confirm(`Add "${tag}" tag ?`));
    },
  },
  argTypes: {
    tagValidationHook: {
      control: { type: 'function' },
    },
  },
};
