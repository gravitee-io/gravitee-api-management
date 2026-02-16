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
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { action } from 'storybook/actions';
import { HttpClientModule } from '@angular/common/http';

import { GioFormListenersTcpHostsComponent } from './gio-form-listeners-tcp-hosts.component';
import { GioFormListenersTcpHostsModule } from './gio-form-listeners-tcp-hosts.module';

export default {
  title: 'API / Listeners / TCP / Form listeners TCP hosts',
  component: GioFormListenersTcpHostsComponent,
  decorators: [
    moduleMetadata({
      imports: [BrowserAnimationsModule, GioFormListenersTcpHostsModule, FormsModule, ReactiveFormsModule, HttpClientModule],
      providers: [],
    }),
  ],
  argTypes: {},
  render: args => ({
    template: `<gio-form-listeners-tcp-hosts [ngModel]="listeners"></gio-form-listeners-tcp-hosts>`,
    props: args,
  }),
  args: {
    listeners: [],
  },
} as Meta;

export const Default: StoryObj = {};
Default.args = {};

export const Filled: StoryObj = {
  args: {
    listeners: [
      {
        host: 'host-1',
      },
      {
        host: 'host-2',
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
      template: `<gio-form-listeners-tcp-hosts [formControl]="formControl"></gio-form-listeners-tcp-hosts>`,
      props: {
        formControl,
      },
    };
  },
  args: {
    listeners: [
      {
        host: 'host-1',
      },
      {
        host: 'host-2',
      },
      {
        host: 'host-3',
      },
    ],
  },
};
