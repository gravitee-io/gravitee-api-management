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
import { EMPTY, of } from 'rxjs';

import { SubscriptionApiKeysComponent } from './subscription-api-keys.component';

import { fakeApplicationSubscriptionApiKey } from '../../../../../../entities/subscription/ApplicationSubscriptionApiKey.fixture';
import { GioTestingPermissionProvider } from '../../../../../../shared/components/gio-permission/gio-permission.service';
import { ApplicationSubscriptionService } from '../../../../../../services-ngx/application-subscription.service';

export default {
  title: 'Application / Subscription / Api keys',
  component: SubscriptionApiKeysComponent,
  argTypes: {},
  render: (args) => ({
    template: `
      <div style="width: 1000px">
        <subscription-api-keys
        [applicationId]="'applicationId'"
        [subscriptionId]="'subscriptionId'"
        [isSharedApiKeyMode]="isSharedApiKeyMode"
        [subscriptionStatus]="subscriptionStatus"
        ></subscription-api-keys>
      </div>
    `,
    props: args,
  }),
  decorators: [
    applicationConfig({
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: [
            'application-subscription-c',
            'application-subscription-r',
            'application-subscription-u',
            'application-subscription-d',
          ],
        },
        {
          provide: ApplicationSubscriptionService,
          useValue: {
            getApiKeys: () => EMPTY,
          },
        },
      ],
    }),
  ],
} as Meta;

export const Loading: StoryObj = {};

export const Empty: StoryObj = {};
Empty.decorators = [
  applicationConfig({
    providers: [
      {
        provide: ApplicationSubscriptionService,
        useValue: {
          getApiKeys: () => of([]),
        },
      },
    ],
  }),
];

const WithApiKeysDecorator = [
  applicationConfig({
    providers: [
      {
        provide: ApplicationSubscriptionService,
        useValue: {
          getApiKeys: () =>
            of([
              fakeApplicationSubscriptionApiKey({
                id: '1',
                key: 'key1',
              }),
              fakeApplicationSubscriptionApiKey({
                id: '2',
                key: 'key2',
                expired: true,
                expire_at: 1712062118650,
              }),
              fakeApplicationSubscriptionApiKey({
                id: '3',
                key: 'key3',
                revoked: true,
                revoked_at: 1712062118650,
              }),
              fakeApplicationSubscriptionApiKey({
                id: '4',
                key: 'key4',
              }),
            ]),
        },
      },
    ],
  }),
];
export const AcceptedSubscription: StoryObj = {};
AcceptedSubscription.decorators = WithApiKeysDecorator;
AcceptedSubscription.args = {
  subscriptionStatus: 'ACCEPTED',
  isSharedApiKeyMode: false,
};

export const AcceptedSharedSubscription: StoryObj = {};
AcceptedSharedSubscription.decorators = WithApiKeysDecorator;
AcceptedSharedSubscription.args = {
  subscriptionStatus: 'ACCEPTED',
  isSharedApiKeyMode: true,
};

export const PendingSubscription: StoryObj = {};
PendingSubscription.decorators = WithApiKeysDecorator;
PendingSubscription.args = {
  subscriptionStatus: 'PENDING',
  isSharedApiKeyMode: false,
};
