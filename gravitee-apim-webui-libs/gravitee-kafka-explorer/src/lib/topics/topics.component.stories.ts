/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { Meta, StoryObj, applicationConfig } from '@storybook/angular';

import { TopicsComponent } from './topics.component';
import { fakeKafkaTopic } from '../models/kafka-cluster.fixture';

const meta: Meta<TopicsComponent> = {
  title: 'Topics',
  component: TopicsComponent,
  decorators: [
    applicationConfig({
      providers: [provideAnimationsAsync()],
    }),
  ],
};

export default meta;
type Story = StoryObj<TopicsComponent>;

export const Default: Story = {
  args: {
    topics: [
      fakeKafkaTopic({
        name: 'orders',
        partitionCount: 6,
        replicationFactor: 3,
        underReplicatedCount: 0,
        size: 5242880,
        messageCount: 12000,
      }),
      fakeKafkaTopic({
        name: 'payments',
        partitionCount: 3,
        replicationFactor: 3,
        underReplicatedCount: 0,
        size: 2097152,
        messageCount: 3500,
      }),
      fakeKafkaTopic({
        name: 'notifications',
        partitionCount: 1,
        replicationFactor: 2,
        underReplicatedCount: 0,
        size: 524288,
        messageCount: 800,
      }),
      fakeKafkaTopic({
        name: '__consumer_offsets',
        partitionCount: 50,
        replicationFactor: 1,
        underReplicatedCount: 0,
        internal: true,
        size: 10485760,
        messageCount: 50000,
      }),
      fakeKafkaTopic({
        name: '__transaction_state',
        partitionCount: 50,
        replicationFactor: 1,
        underReplicatedCount: 0,
        internal: true,
        size: 1048576,
        messageCount: 200,
      }),
    ],
    totalElements: 5,
    page: 0,
    pageSize: 25,
  },
};

export const WithUnderReplicated: Story = {
  args: {
    topics: [
      fakeKafkaTopic({
        name: 'orders',
        partitionCount: 6,
        replicationFactor: 3,
        underReplicatedCount: 2,
        size: 5242880,
        messageCount: 12000,
      }),
      fakeKafkaTopic({
        name: 'payments',
        partitionCount: 3,
        replicationFactor: 3,
        underReplicatedCount: 0,
        size: 2097152,
        messageCount: 3500,
      }),
      fakeKafkaTopic({
        name: 'events',
        partitionCount: 12,
        replicationFactor: 3,
        underReplicatedCount: 5,
        size: 52428800,
        messageCount: 100000,
      }),
    ],
    totalElements: 3,
    page: 0,
    pageSize: 25,
  },
};

export const Empty: Story = {
  args: {
    topics: [],
    totalElements: 0,
    page: 0,
    pageSize: 25,
  },
};
