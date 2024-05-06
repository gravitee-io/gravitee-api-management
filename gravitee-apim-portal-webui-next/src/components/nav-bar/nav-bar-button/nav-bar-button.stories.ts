/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { APP_BASE_HREF } from '@angular/common';
import { MatButton } from '@angular/material/button';
import { provideRouter } from '@angular/router';
import { applicationConfig, Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { NavBarButtonComponent } from './nav-bar-button.component';
import { resetTheme } from '../../../stories/theme/theme.util';

export default {
  title: 'Nav Bar Button',
  decorators: [
    applicationConfig({
      providers: [provideRouter([{ path: '', component: MatButton }])],
    }),
    moduleMetadata({
      imports: [NavBarButtonComponent],
      providers: [
        {
          provide: APP_BASE_HREF,
          useValue: '/',
        },
      ],
    }),
  ],
  render: () => ({}),
} as Meta;

export const Simple: StoryObj = {
  render: () => {
    resetTheme();
    return {
      template: `
        <div style="display: flex; gap: 24px;">
          <app-nav-bar-button [path]="['inactive']">Simple label of inactive page</app-nav-bar-button>
        </div>
        `,
      styles: [],
    };
  },
};

export const WithTranslation: StoryObj = {
  render: () => ({
    template: `
        <div style="display: flex; gap: 24px;">
          <app-nav-bar-button i18n [path]="['inactive']">Label that has translation</app-nav-bar-button>
          <app-nav-bar-button i18n="@@myStoryId" [path]="['inactive']">Label that has translation id</app-nav-bar-button>
        </div>
        `,
    styles: [],
  }),
};

export const Active: StoryObj = {
  render: () => ({
    template: `
        <div style="display: flex; gap: 24px;">
          <app-nav-bar-button [path]="['']">Active page</app-nav-bar-button>
        </div>
        `,
    styles: [],
  }),
};
