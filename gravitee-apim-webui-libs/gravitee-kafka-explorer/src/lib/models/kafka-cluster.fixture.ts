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
import { DescribeClusterResponse, KafkaNode } from './kafka-cluster.model';

export function fakeKafkaNode(overrides: Partial<KafkaNode> = {}): KafkaNode {
  return {
    id: 0,
    host: 'kafka-broker-0.example.com',
    port: 9092,
    ...overrides,
  };
}

export function fakeDescribeClusterResponse(overrides: Partial<DescribeClusterResponse> = {}): DescribeClusterResponse {
  return {
    clusterId: 'test-cluster-id',
    controller: fakeKafkaNode({ id: 0 }),
    nodes: [
      fakeKafkaNode({ id: 0, host: 'kafka-broker-0.example.com' }),
      fakeKafkaNode({ id: 1, host: 'kafka-broker-1.example.com' }),
      fakeKafkaNode({ id: 2, host: 'kafka-broker-2.example.com' }),
    ],
    ...overrides,
  };
}
