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
import { Component } from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter, RouterOutlet } from '@angular/router';
import { Meta, StoryObj, applicationConfig } from '@storybook/angular';
import { EMPTY, Observable, delay, of, throwError } from 'rxjs';

import { KAFKA_EXPLORER_ROUTES } from './kafka-explorer.routes';
import {
  fakeBrokerDetail,
  fakeDescribeClusterResponse,
  fakeDescribeTopicResponse,
  fakeKafkaNode,
  fakeKafkaTopic,
  fakeListTopicsResponse,
  fakePagination,
} from '../models/kafka-cluster.fixture';
import { DescribeClusterResponse, DescribeTopicResponse, ListTopicsResponse } from '../models/kafka-cluster.model';
import { KAFKA_EXPLORER_BASE_URL } from '../services/kafka-explorer-config.token';

function mockSuccessInterceptor(
  clusterResponse: DescribeClusterResponse,
  topicsResponse: ListTopicsResponse,
  topicDetailResponse: DescribeTopicResponse = fakeDescribeTopicResponse(),
): HttpInterceptorFn {
  return (req: HttpRequest<unknown>, _next: HttpHandlerFn): Observable<HttpEvent<unknown>> => {
    if (req.url.includes('describe-topic')) {
      return of(new HttpResponse({ status: 200, body: topicDetailResponse })).pipe(delay(200));
    }
    if (req.url.includes('list-topics')) {
      return of(new HttpResponse({ status: 200, body: topicsResponse })).pipe(delay(0));
    }
    return of(new HttpResponse({ status: 200, body: clusterResponse })).pipe(delay(0));
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

@Component({
  standalone: true,
  imports: [RouterOutlet],
  template: `<router-outlet />`,
})
class StoryHostComponent {}

const meta: Meta<StoryHostComponent> = {
  title: 'KafkaExplorer',
  component: StoryHostComponent,
};

export default meta;
type Story = StoryObj<StoryHostComponent>;

const defaultTopics = fakeListTopicsResponse({
  data: [
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
      name: 'events',
      partitionCount: 12,
      replicationFactor: 3,
      underReplicatedCount: 2,
      size: 52428800,
      messageCount: 100000,
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
  ],
  pagination: fakePagination({ pageItemsCount: 5, totalCount: 5 }),
});

function storyProviders(interceptor: HttpInterceptorFn) {
  return [
    provideAnimationsAsync(),
    provideHttpClient(withInterceptors([interceptor])),
    provideRouter([
      {
        path: 'clusters/:clusterId',
        children: [{ path: 'explorer', children: KAFKA_EXPLORER_ROUTES }],
      },
      { path: '**', redirectTo: 'clusters/my-cluster/explorer' },
    ]),
    { provide: KAFKA_EXPLORER_BASE_URL, useValue: '/api/v2' },
  ];
}

export const Default: Story = {
  decorators: [
    applicationConfig({
      providers: storyProviders(
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
          defaultTopics,
        ),
      ),
    }),
  ],
};

export const SingleNode: Story = {
  decorators: [
    applicationConfig({
      providers: storyProviders(
        mockSuccessInterceptor(
          fakeDescribeClusterResponse({
            nodes: [fakeBrokerDetail({ id: 0, host: 'kafka-broker-0.example.com' })],
            controller: fakeKafkaNode({ id: 0 }),
            totalTopics: 3,
            totalPartitions: 12,
          }),
          fakeListTopicsResponse({
            data: [
              fakeKafkaTopic({ name: 'my-topic', partitionCount: 3, replicationFactor: 1, size: 1048576, messageCount: 1500 }),
              fakeKafkaTopic({
                name: '__consumer_offsets',
                partitionCount: 50,
                replicationFactor: 1,
                internal: true,
                size: 10485760,
                messageCount: 50000,
              }),
            ],
            pagination: fakePagination({ pageItemsCount: 2, totalCount: 2 }),
          }),
        ),
      ),
    }),
  ],
};

export const Loading: Story = {
  decorators: [
    applicationConfig({
      providers: storyProviders(mockLoadingInterceptor()),
    }),
  ],
};

export const Error: Story = {
  decorators: [
    applicationConfig({
      providers: storyProviders(mockErrorInterceptor('Failed to connect to the Kafka cluster. Please verify the cluster configuration.')),
    }),
  ],
};
