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
import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';
import { action } from 'storybook/actions';
import { MatFormFieldModule } from '@angular/material/form-field';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';

import { GioFormColorInputComponent } from './gio-form-color-input.component';
import { GioFormColorInputModule } from './gio-form-color-input.module';

export default {
  title: 'Shared / Form color input',
  component: GioFormColorInputComponent,
  decorators: [
    moduleMetadata({
      imports: [BrowserAnimationsModule, GioFormColorInputModule, FormsModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule],
    }),
  ],
  render: () => ({}),
  argTypes: {
    color: {
      control: { type: 'color' },
    },
    placeholder: {
      control: { type: 'text' },
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

export const Simple: StoryObj = {
  render: ({ color, placeholder, required, disabled }) => {
    const colorControl = new FormControl({ value: color, disabled });

    colorControl.valueChanges.subscribe(value => {
      action('Color')(value);
    });

    return {
      template: `
        <mat-form-field style="width:100%">
          <mat-label>Select color</mat-label>
          <gio-form-color-input [required]="required" [placeholder]="placeholder" [formControl]="colorControl">
          </gio-form-color-input>
          <mat-error *ngIf="colorControl.hasError('color')">
            {{ colorControl.getError('color').message }}
          </mat-error>
        </mat-form-field>
      `,
      props: {
        colorControl,
        placeholder,
        required,
        disabled,
      },
    };
  },
  args: {},
};

export const Disabled: StoryObj = {
  render: Simple.render,
  args: {
    disabled: true,
    color: '#ff00c8',
  },
};

export const Validation: StoryObj = {
  render: Simple.render,
  args: {
    color: 'aaaaa',
  },
};
