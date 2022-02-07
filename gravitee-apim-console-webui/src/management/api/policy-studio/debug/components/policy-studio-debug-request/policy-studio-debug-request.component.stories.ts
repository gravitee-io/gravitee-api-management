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
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Meta, moduleMetadata } from '@storybook/angular';
import { Story } from '@storybook/angular/dist/ts3.9/client/preview/types-7-0';

import { PolicyStudioDebugRequestComponent } from './policy-studio-debug-request.component';

export default {
  title: 'APIM / Policy Studio / Debug / Components / Request',
  component: PolicyStudioDebugRequestComponent,
  decorators: [
    moduleMetadata({
      imports: [
        CommonModule,
        BrowserAnimationsModule,

        ReactiveFormsModule,

        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        MatButtonModule,
        MatTabsModule,
      ],
    }),
  ],
  argTypes: {
    onRequestSubmitted: { action: 'onRequestSubmitted' },
  },
  render: ({ onRequestSubmitted }) => ({
    props: { onRequestSubmitted },
    template: '<policy-studio-debug-request (requestSubmitted)="onRequestSubmitted($event)"></policy-studio-debug-request>',
  }),
} as Meta;

export const Default: Story = {};
