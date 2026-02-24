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
import {
  HttpEvent,
  HttpHandlerFn,
  HttpInterceptorFn,
  HttpRequest,
  HttpResponse,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { Meta, StoryObj, applicationConfig } from '@storybook/angular';
import { EMPTY, Observable, delay, of, throwError } from 'rxjs';

import { KafkaExplorerComponent } from './kafka-explorer.component';
import { fakeBrokerDetail, fakeDescribeClusterResponse, fakeKafkaNode } from '../models/kafka-cluster.fixture';
import { DescribeClusterResponse } from '../models/kafka-cluster.model';

function mockSuccessInterceptor(response: DescribeClusterResponse): HttpInterceptorFn {
  return (_req: HttpRequest<unknown>, _next: HttpHandlerFn): Observable<HttpEvent<unknown>> => {
    return of(new HttpResponse({ status: 200, body: response })).pipe(delay(0));
  };
}

function mockLoadingInterceptor(): HttpInterceptorFn {
  return (): Observable<HttpEvent<unknown>> => EMPTY;
}

function mockErrorInterceptor(message: string): HttpInterceptorFn {
  return (): Observable<HttpEvent<unknown>> => {
    return throwError(() => ({ error: { message } }));
  };
}

const meta: Meta<KafkaExplorerComponent> = {
  title: 'KafkaExplorer',
  component: KafkaExplorerComponent,
};

export default meta;
type Story = StoryObj<KafkaExplorerComponent>;

export const Default: Story = {
  args: { baseURL: '/api/v2', clusterId: 'my-cluster' },
  decorators: [
    applicationConfig({
      providers: [
        provideAnimationsAsync(),
        provideHttpClient(
          withInterceptors([
            mockSuccessInterceptor(
              fakeDescribeClusterResponse({
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
                totalTopics: 42,
                totalPartitions: 150,
              }),
            ),
          ]),
        ),
      ],
    }),
  ],
};

export const SingleNode: Story = {
  args: { baseURL: '/api/v2', clusterId: 'my-cluster' },
  decorators: [
    applicationConfig({
      providers: [
        provideAnimationsAsync(),
        provideHttpClient(
          withInterceptors([
            mockSuccessInterceptor(
              fakeDescribeClusterResponse({
                nodes: [fakeBrokerDetail({ id: 0, host: 'kafka-broker-0.example.com' })],
                controller: fakeKafkaNode({ id: 0 }),
                totalTopics: 3,
                totalPartitions: 12,
              }),
            ),
          ]),
        ),
      ],
    }),
  ],
};

export const Loading: Story = {
  args: { baseURL: '/api/v2', clusterId: 'my-cluster' },
  decorators: [
    applicationConfig({
      providers: [provideAnimationsAsync(), provideHttpClient(withInterceptors([mockLoadingInterceptor()]))],
    }),
  ],
};

export const Error: Story = {
  args: { baseURL: '/api/v2', clusterId: 'my-cluster' },
  decorators: [
    applicationConfig({
      providers: [
        provideAnimationsAsync(),
        provideHttpClient(
          withInterceptors([mockErrorInterceptor('Failed to connect to the Kafka cluster. Please verify the cluster configuration.')]),
        ),
      ],
    }),
  ],
};
