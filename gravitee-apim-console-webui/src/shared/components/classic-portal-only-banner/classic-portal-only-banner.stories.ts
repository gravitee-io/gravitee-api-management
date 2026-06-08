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
import { applicationConfig, Meta, StoryObj } from '@storybook/angular';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { ClassicPortalOnlyBannerComponent } from './classic-portal-only-banner.component';

import { EnvironmentSettingsService } from '../../../services-ngx/environment-settings.service';
import { GioPermissionService } from '../gio-permission/gio-permission.service';

const withPortalNextDisabled = applicationConfig({
  providers: [
    provideRouter([]),
    { provide: EnvironmentSettingsService, useValue: { isPortalNextEnabled: () => of(false) } },
    { provide: GioPermissionService, useValue: { hasAnyMatching: () => false } },
    { provide: ActivatedRoute, useValue: { snapshot: { params: {} } } },
  ],
});

const withPortalNextEnabledNoAction = applicationConfig({
  providers: [
    provideRouter([]),
    { provide: EnvironmentSettingsService, useValue: { isPortalNextEnabled: () => of(true) } },
    { provide: GioPermissionService, useValue: { hasAnyMatching: () => false } },
    { provide: ActivatedRoute, useValue: { snapshot: { params: {} } } },
  ],
});

const withPortalNextEnabledAndAction = applicationConfig({
  providers: [
    provideRouter([]),
    { provide: EnvironmentSettingsService, useValue: { isPortalNextEnabled: () => of(true) } },
    { provide: GioPermissionService, useValue: { hasAnyMatching: () => true } },
    { provide: ActivatedRoute, useValue: { snapshot: { params: { envHrid: 'my-env' } } } },
  ],
});

export default {
  title: 'Shared / Classic Portal Only Banner',
  component: ClassicPortalOnlyBannerComponent,
  render: ({ title, body, actionLabel }) => ({
    props: { title, body, actionLabel },
  }),
} as Meta;

export const Default: StoryObj = {
  decorators: [withPortalNextDisabled],
  args: {
    title: 'Classic Developer Portal Only',
    body: '',
    actionLabel: 'Next Gen Portal Settings',
  },
};

export const WithBody: StoryObj = {
  decorators: [withPortalNextEnabledNoAction],
  args: {
    title: 'Classic Developer Portal Only',
    body: 'This documentation is used by the Classic Developer Portal. Manage Next Gen Portal API content in Next Gen Portal settings.',
    actionLabel: 'Next Gen Portal Settings',
  },
};

export const WithAction: StoryObj = {
  decorators: [withPortalNextEnabledAndAction],
  args: {
    title: 'Classic Developer Portal Only',
    body: 'This documentation is used by the Classic Developer Portal. Manage Next Gen Portal API content in Next Gen Portal settings.',
    actionLabel: 'Next Gen Portal Settings',
  },
};

export const ApiConfiguration: StoryObj = {
  decorators: [withPortalNextEnabledAndAction],
  args: {
    title: 'Classic Developer Portal Only',
    body: 'These actions only affect Classic Developer Portal visibility and publication. Manage Next Gen Portal API visibility in Next Gen Portal settings.',
    actionLabel: 'Next Gen Portal Settings',
  },
};
