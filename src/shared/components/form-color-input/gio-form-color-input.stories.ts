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
import { Meta, moduleMetadata, Story } from '@storybook/angular';
import { action } from '@storybook/addon-actions';
import { MatFormFieldModule } from '@angular/material/form-field';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { GioFormColorInputComponent } from './gio-form-color-input.component';
import { GioFormColorInputModule } from './gio-form-color-input.module';

export default {
  title: 'Shared / Form color input',
  component: GioFormColorInputComponent,
  decorators: [
    moduleMetadata({
      imports: [BrowserAnimationsModule, GioFormColorInputModule, FormsModule, ReactiveFormsModule, MatFormFieldModule],
    }),
  ],
  render: () => ({}),
  argTypes: {
    color: {
      control: { type: 'string' },
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

export const Simple: Story = {
  render: ({ color, placeholder, required, disabled }) => ({
    template: `
      <gio-form-color-input [disabled]="disabled" [required]="required" [placeholder]="placeholder" [ngModel]="color" (ngModelChange)="onColorChange($event)">
      </gio-form-color-input>
    `,
    props: {
      color,
      placeholder,
      required,
      disabled,
      onColorChange: (e) => action('Color')(e),
    },
  }),
  args: {},
};
