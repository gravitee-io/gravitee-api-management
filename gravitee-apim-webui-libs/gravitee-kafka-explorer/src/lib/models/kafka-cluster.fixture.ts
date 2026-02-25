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
import { BrokerDetail, DescribeClusterResponse, KafkaNode, KafkaTopic, ListTopicsResponse, Pagination } from './kafka-cluster.model';

export function fakeKafkaNode(overrides: Partial<KafkaNode> = {}): KafkaNode {
  return {
    id: 0,
    host: 'kafka-broker-0.example.com',
    port: 9092,
    ...overrides,
  };
}

export function fakeBrokerDetail(overrides: Partial<BrokerDetail> = {}): BrokerDetail {
  return {
    id: 0,
    host: 'kafka-broker-0.example.com',
    port: 9092,
    leaderPartitions: 10,
    replicaPartitions: 20,
    logDirSize: 1073741824,
    ...overrides,
  };
}

export function fakeDescribeClusterResponse(overrides: Partial<DescribeClusterResponse> = {}): DescribeClusterResponse {
  return {
    clusterId: 'test-cluster-id',
    controller: fakeKafkaNode({ id: 0 }),
    nodes: [
      fakeBrokerDetail({ id: 0, host: 'kafka-broker-0.example.com' }),
      fakeBrokerDetail({ id: 1, host: 'kafka-broker-1.example.com' }),
      fakeBrokerDetail({ id: 2, host: 'kafka-broker-2.example.com' }),
    ],
    totalTopics: 5,
    totalPartitions: 15,
    ...overrides,
  };
}

export function fakeKafkaTopic(overrides: Partial<KafkaTopic> = {}): KafkaTopic {
  return {
    name: 'my-topic',
    partitionCount: 3,
    replicationFactor: 2,
    underReplicatedCount: 0,
    internal: false,
    size: 1048576,
    messageCount: 1500,
    ...overrides,
  };
}

export function fakePagination(overrides: Partial<Pagination> = {}): Pagination {
  return {
    page: 1,
    perPage: 10,
    pageCount: 1,
    pageItemsCount: 3,
    totalCount: 3,
    ...overrides,
  };
}

export function fakeListTopicsResponse(overrides: Partial<ListTopicsResponse> = {}): ListTopicsResponse {
  return {
    data: [
      fakeKafkaTopic({ name: 'my-topic' }),
      fakeKafkaTopic({ name: 'orders', partitionCount: 6, replicationFactor: 3, size: 5242880, messageCount: 12000 }),
      fakeKafkaTopic({
        name: '__consumer_offsets',
        partitionCount: 50,
        replicationFactor: 1,
        internal: true,
        size: 10485760,
        messageCount: 50000,
      }),
    ],
    pagination: fakePagination(),
    ...overrides,
  };
}
