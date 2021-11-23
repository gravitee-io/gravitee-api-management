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
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Meta, moduleMetadata } from '@storybook/angular';
import { Story } from '@storybook/angular/dist/ts3.9/client/preview/types-7-0';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { GioClipboardModule } from './gio-clipboard.module';
import { GioClipboardCopyWrapperComponent } from './gio-clipboard-copy-wrapper.component';

export default {
  title: 'Shared / Clipboard',
  component: GioClipboardCopyWrapperComponent,
  decorators: [
    moduleMetadata({
      imports: [BrowserAnimationsModule, GioClipboardModule, MatFormFieldModule, MatInputModule],
    }),
  ],
} as Meta;

export const InnerButton: Story = {
  render: () => {
    return {
      template: `<div style="width:300px;"><div gioClipboardCopyWrapper contentToCopy="Hello" > Copy me ! </div></div>`,
      props: {},
    };
  },
};

export const InnerButtonAlwaysVisible: Story = {
  render: () => {
    return {
      template: `<div style="width:300px;"><div gioClipboardCopyWrapper contentToCopy="Hello" alwaysVisible="true" > Copy me ! </div></div>`,
      props: {},
    };
  },
};

export const FormField: Story = {
  render: () => {
    return {
      template: `
      <mat-form-field  appearance="fill">
        <mat-label>Default animal</mat-label>
        <input #animalInput matInput value="🦊"/>
        <gio-clipboard-copy-icon matSuffix [contentToCopy]="animalInput.value"></gio-clipboard-copy-icon>
      </mat-form-field>
      `,
      props: {},
    };
  },
};
