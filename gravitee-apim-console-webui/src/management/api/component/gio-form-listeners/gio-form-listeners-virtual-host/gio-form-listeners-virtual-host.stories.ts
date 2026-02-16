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
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { action } from 'storybook/actions';
import { of } from 'rxjs';

import { GioFormListenersVirtualHostComponent } from './gio-form-listeners-virtual-host.component';
import { GioFormListenersVirtualHostModule } from './gio-form-listeners-virtual-host.module';

import { PortalConfigurationService } from '../../../../../services-ngx/portal-configuration.service';
import { ApiService } from '../../../../../services-ngx/api.service';
export default {
  title: 'API / Listeners / HTTP / Form listeners virtual host',
  component: GioFormListenersVirtualHostComponent,
  decorators: [
    moduleMetadata({
      imports: [BrowserAnimationsModule, GioFormListenersVirtualHostModule, FormsModule, ReactiveFormsModule],
      providers: [
        { provide: PortalConfigurationService, useValue: { get: () => of({ portal: { entrypoint: '' } }) } },
        { provide: ApiService, useValue: { verify: () => of() } },
      ],
    }),
  ],
  argTypes: {},
  render: args => ({
    template: `<gio-form-listeners-virtual-host [ngModel]="listeners" [domainRestrictions]="domainRestrictions"></gio-form-listeners-virtual-host>`,
    props: args,
  }),
  args: {
    listeners: [],
    domainRestrictions: [],
  },
} as Meta;

export const Default: StoryObj = {};
Default.args = {};

export const Filled: StoryObj = {
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

export const ReactiveForm: StoryObj = {
  render: args => {
    const formControl = new FormControl(args.listeners);

    formControl.valueChanges.subscribe(value => {
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

export const WithDomainRestrictions: StoryObj = {};
WithDomainRestrictions.args = {
  domainRestrictions: ['gravitee.io', 'graviteesource.com'],
};
