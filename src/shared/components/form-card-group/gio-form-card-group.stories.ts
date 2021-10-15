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
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MatCardModule } from '@angular/material/card';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { action } from '@storybook/addon-actions';

import { GioFormCardGroupModule } from './gio-form-card-group.module';
import { GioFormCardGroupComponent } from './gio-form-card-group.component';

export default {
  title: 'Shared / Form card group',
  component: GioFormCardGroupComponent,
  decorators: [
    moduleMetadata({
      imports: [FormsModule, ReactiveFormsModule, BrowserAnimationsModule, GioFormCardGroupModule, MatCardModule],
    }),
  ],
  render: () => ({}),
} as Meta;

export const Simple: Story = {
  render: () => ({
    template: `
    <gio-form-card-group [ngModel]="selected" (ngModelChange)="onSelect($event)">
      <gio-form-card value="A">Hello</gio-form-card>
      <gio-form-card value="B">Hello</gio-form-card>
      <gio-form-card value="C">Hello</gio-form-card>
      <gio-form-card value="D">Hello</gio-form-card>
      <gio-form-card value="E">Hello</gio-form-card>
    </gio-form-card-group>
    `,
    props: {
      selected: 'A',
      onSelect: (e) => action('On select')(e),
    },
  }),
};

export const ReactiveForms: Story = {
  render: () => {
    const selectControl = new FormControl('A');

    selectControl.valueChanges.subscribe((value) => {
      action('On select')(value);
    });

    return {
      template: `
      <gio-form-card-group [formControl]="selectControl">
        <gio-form-card value="A">Hello</gio-form-card>
        <gio-form-card value="B">Hello</gio-form-card>
        <gio-form-card value="C">Hello</gio-form-card>
        <gio-form-card value="D">Hello</gio-form-card>
        <gio-form-card value="E">Hello</gio-form-card>
      </gio-form-card-group>
      `,
      props: {
        selectControl,
      },
    };
  },
};
