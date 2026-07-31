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
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { BrandedSendersComponent } from './branded-senders.component';

import { BrandedSender } from '../../../entities/brandedSender';

export default {
  title: 'Shared / Branded senders',
  component: BrandedSendersComponent,
  decorators: [
    moduleMetadata({
      imports: [ReactiveFormsModule, BrandedSendersComponent],
    }),
  ],
} as Meta;

const defaults = {
  defaultFrom: 'noreply@example.com',
  defaultSubject: '[Example] %s',
};

const template = `<branded-senders [formControl]="control" [defaultFrom]="defaultFrom" [defaultSubject]="defaultSubject" />`;

export const Empty: StoryObj = {
  render: () => ({
    template,
    props: { control: new FormControl<BrandedSender[]>([]), ...defaults },
  }),
};

export const Prefilled: StoryObj = {
  render: () => ({
    template,
    props: {
      control: new FormControl<BrandedSender[]>([
        { domains: ['example.com', 'eu.example.com'], from: 'noreply@example.com', subject: '[Example] %s' },
        { domains: ['example.org'], from: 'Partner Team <noreply@example.org>', subject: '[Partner] %s' },
      ]),
      ...defaults,
    },
  }),
};

export const Disabled: StoryObj = {
  render: () => ({
    template,
    props: {
      control: new FormControl<BrandedSender[]>({
        value: [{ domains: ['example.com'], from: 'noreply@example.com', subject: '[Example] %s' }],
        disabled: true,
      }),
      ...defaults,
    },
  }),
};

export const InheritedFromOrg: StoryObj = {
  render: () => ({
    template: `<branded-senders [formControl]="control" [defaultFrom]="defaultFrom" [defaultSubject]="defaultSubject" [inheritedFromOrg]="true" />`,
    props: {
      control: new FormControl<BrandedSender[]>([
        { domains: ['example.com', 'eu.example.com'], from: 'noreply@example.com', subject: '[Example] %s' },
      ]),
      ...defaults,
    },
  }),
};
