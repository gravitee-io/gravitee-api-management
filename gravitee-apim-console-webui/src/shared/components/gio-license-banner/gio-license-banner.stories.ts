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
import { License } from '@gravitee/ui-particles-angular';
import { action } from '@storybook/addon-actions';

import { GioLicenseBannerComponent } from './gio-license-banner.component';
import { GioLicenseBannerModule } from './gio-license-banner.module';

const OSS_LICENSE: License = {
  tier: 'oss',
  packs: [],
  features: [],
  scope: 'PLATFORM',
};
const EE_LICENSE: License = {
  tier: 'universe',
  packs: [],
  features: [],
  scope: 'PLATFORM',
};
const CLOUD_LICENSE: License = {
  tier: 'universe',
  packs: [],
  features: [],
  scope: 'ORGANIZATION',
};
const OEM_LICENSE: License = {
  tier: 'universe',
  packs: [],
  features: ['oem-customization'],
  scope: 'ORGANIZATION',
};

export default {
  title: 'Shared / Gio License banner',
  component: GioLicenseBannerComponent,
  decorators: [
    moduleMetadata({
      imports: [GioLicenseBannerModule],
    }),
  ],
  argTypes: {
    onRequestUpgrade: { action: 'on-request-upgrade' },
  },
} as Meta;

export const NoLicense: StoryObj = {
  args: {
    onRequestUpgrade: action('on-request-upgrade'),
  },
  render: ({ onRequestUpgrade }) => {
    return {
      props: {
        license: null,
        isOEM: false,
        onRequestUpgrade,
      },
      template: `<gio-license-banner [license]="license" [isOEM]="isOEM" (onRequestUpgrade)="onRequestUpgrade()"></gio-license-banner>`,
    };
  },
};
export const OSSLicense: StoryObj = {
  args: {
    onRequestUpgrade: action('on-request-upgrade'),
  },
  render: ({ onRequestUpgrade }) => {
    return {
      props: {
        license: OSS_LICENSE,
        isOEM: false,
        onRequestUpgrade,
      },
      template: `<gio-license-banner [license]="license" [isOEM]="isOEM" (onRequestUpgrade)="onRequestUpgrade()"></gio-license-banner>`,
    };
  },
};
export const OEMLicense: StoryObj = {
  args: {
    onRequestUpgrade: action('on-request-upgrade'),
  },
  render: ({ onRequestUpgrade }) => {
    return {
      props: {
        license: OEM_LICENSE,
        isOEM: true,
        onRequestUpgrade,
      },
      template: `<gio-license-banner [license]="license" [isOEM]="isOEM" (onRequestUpgrade)="onRequestUpgrade()"></gio-license-banner>`,
    };
  },
};
export const EELicense: StoryObj = {
  args: {
    onRequestUpgrade: action('on-request-upgrade'),
  },
  render: ({ onRequestUpgrade }) => {
    return {
      props: {
        license: EE_LICENSE,
        isOEM: false,
        onRequestUpgrade,
      },
      template: `<gio-license-banner [license]="license" [isOEM]="isOEM" (onRequestUpgrade)="onRequestUpgrade()"></gio-license-banner>`,
    };
  },
};
export const CloudLicense: StoryObj = {
  args: {
    onRequestUpgrade: action('on-request-upgrade'),
  },
  render: ({ onRequestUpgrade }) => {
    return {
      props: {
        license: CLOUD_LICENSE,
        isOEM: false,
        onRequestUpgrade,
      },
      template: `<gio-license-banner [license]="license" [isOEM]="isOEM" (onRequestUpgrade)="onRequestUpgrade()"></gio-license-banner>`,
    };
  },
};
