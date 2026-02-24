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

import { BrokersComponent } from './brokers.component';
import { fakeBrokerDetail } from '../models/kafka-cluster.fixture';

const meta: Meta<BrokersComponent> = {
  title: 'Brokers',
  component: BrokersComponent,
  decorators: [
    applicationConfig({
      providers: [provideAnimationsAsync()],
    }),
  ],
};

export default meta;
type Story = StoryObj<BrokersComponent>;

export const Default: Story = {
  args: {
    nodes: [
      fakeBrokerDetail({
        id: 0,
        host: 'kafka-broker-0.example.com',
        rack: 'us-east-1a',
        leaderPartitions: 50,
        replicaPartitions: 100,
        logDirSize: 5368709120,
      }),
      fakeBrokerDetail({
        id: 1,
        host: 'kafka-broker-1.example.com',
        rack: 'us-east-1b',
        leaderPartitions: 48,
        replicaPartitions: 100,
        logDirSize: 4294967296,
      }),
      fakeBrokerDetail({
        id: 2,
        host: 'kafka-broker-2.example.com',
        rack: 'us-east-1c',
        leaderPartitions: 52,
        replicaPartitions: 100,
        logDirSize: 5905580032,
      }),
    ],
    controllerId: 0,
  },
};

export const SingleNode: Story = {
  args: {
    nodes: [
      fakeBrokerDetail({
        id: 0,
        host: 'kafka-broker-0.example.com',
        leaderPartitions: 150,
        replicaPartitions: 150,
        logDirSize: 10737418240,
      }),
    ],
    controllerId: 0,
  },
};

export const NoRack: Story = {
  args: {
    nodes: [
      fakeBrokerDetail({ id: 0, host: 'kafka-broker-0.example.com', rack: null, logDirSize: null }),
      fakeBrokerDetail({ id: 1, host: 'kafka-broker-1.example.com', rack: null, logDirSize: null }),
    ],
    controllerId: 1,
  },
};
