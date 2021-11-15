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
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';
import { Story } from '@storybook/angular/dist/ts3.9/client/preview/types-7-0';

import { GioConfirmDialogComponent, GioConfirmDialogData } from './gio-confirm-dialog.component';
import { GioConfirmDialogModule } from './gio-confirm-dialog.module';

export default {
  title: 'Shared / Confirm dialog',
  component: GioConfirmDialogComponent,
  decorators: [
    moduleMetadata({
      imports: [GioConfirmDialogModule, MatDialogModule],
      providers: [
        {
          provide: MatDialogRef,
          useValue: {},
        },
      ],
    }),
  ],
  argTypes: {
    title: {
      type: { name: 'string', required: false },
    },
    content: {
      type: { name: 'string', required: false },
    },
    confirmButton: {
      type: { name: 'string', required: false },
    },
    cancelButton: {
      type: { name: 'string', required: false },
    },
  },
  render: (args) => ({
    props: {},
    moduleMetadata: {
      providers: [
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            title: args.title,
            content: args.content,
            confirmButton: args.confirmButton,
            cancelButton: args.cancelButton,
          } as GioConfirmDialogData,
        },
      ],
    },
  }),
} as Meta;

export const Default: StoryObj = {};

export const Custom: Story<GioConfirmDialogData> = {};
Custom.args = {
  title: 'Are you sure you want to remove all cats ?',
  content: '🙀😿',
  confirmButton: 'Yes, I want',
  cancelButton: 'Nope',
};
