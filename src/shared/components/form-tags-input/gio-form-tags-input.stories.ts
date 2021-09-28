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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { GioFormTagsInputComponent } from './gio-form-tags-input.component';
import { GioFormTagsInputModule } from './gio-form-tags-input.module';

export default {
  title: 'Shared / Form tags input',
  component: GioFormTagsInputComponent,
  decorators: [
    moduleMetadata({
      imports: [BrowserAnimationsModule, GioFormTagsInputModule, MatSlideToggleModule, MatFormFieldModule, MatInputModule, MatIconModule],
    }),
  ],
  render: () => ({}),
} as Meta;

export const Default: Story = {
  render: () => ({
    template: `
      <gio-form-tags-input>
      </gio-form-tags-input>
    `,
  }),
};
