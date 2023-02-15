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
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { action } from '@storybook/addon-actions';
import { of } from 'rxjs';

import { GioFormListenersVirtualHostComponent } from './gio-form-listeners-virtual-host.component';
import { GioFormListenersVirtualHostModule } from './gio-form-listeners-virtual-host.module';

import { PortalSettingsService } from '../../../services-ngx/portal-settings.service';
import { ApiService } from '../../../services-ngx/api.service';
export default {
  title: 'Shared / Form listeners virtual host',
  component: GioFormListenersVirtualHostComponent,
  decorators: [
    moduleMetadata({
      imports: [BrowserAnimationsModule, GioFormListenersVirtualHostModule, FormsModule, ReactiveFormsModule],
      providers: [
        { provide: PortalSettingsService, useValue: { get: () => of({ portal: { entrypoint: '' } }) } },
        { provide: ApiService, useValue: { contextPathValidator: () => () => of() } },
      ],
    }),
  ],
  argTypes: {},
  render: (args) => ({
    template: `<gio-form-listeners-virtual-host [ngModel]="listeners" [domainRestrictions]="domainRestrictions"></gio-form-listeners-virtual-host>`,
    props: args,
  }),
  args: {
    listeners: [],
    domainRestrictions: [],
  },
} as Meta;

export const Default: Story = {};
Default.args = {};

export const Filled: Story = {
  args: {
    listeners: [
      {
        host: 'api.gravitee.io',
        path: '/api/api-1',
        overrideAccess: false,
      },
      {
        host: 'hostname',
        path: '/api/api-1',
        overrideAccess: true,
      },
    ],
  },
};

export const ReactiveForm: Story = {
  render: (args) => {
    const formControl = new FormControl(args.listeners);

    formControl.valueChanges.subscribe((value) => {
      action('Listeners')(value);
    });

    return {
      template: `<gio-form-listeners-virtual-host [formControl]="formControl"></gio-form-listeners-virtual-host>`,
      props: {
        formControl,
      },
    };
  },
  args: {
    listeners: [
      {
        host: 'api.gravitee.io',
        path: '/api/api-1',
        overrideAccess: false,
      },
      {
        host: 'hostname',
        path: '/api/api-1',
        overrideAccess: true,
      },
    ],
    disabled: false,
  },
};

export const WithDomainRestrictions: Story = {};
WithDomainRestrictions.args = {
  domainRestrictions: ['gravitee.io', 'graviteesource.com'],
};
