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
export interface KafkaNode {
  id: number;
  host: string;
  port: number;
}

export interface BrokerDetail {
  id: number;
  host: string;
  port: number;
  rack?: string;
  leaderPartitions: number;
  replicaPartitions: number;
  logDirSize?: number;
}

export interface DescribeClusterResponse {
  clusterId: string;
  controller: KafkaNode;
  nodes: BrokerDetail[];
  totalTopics: number;
  totalPartitions: number;
}

export interface KafkaTopic {
  name: string;
  partitionCount: number;
  replicationFactor: number;
  underReplicatedCount: number;
  internal: boolean;
  size?: number;
  messageCount?: number;
}

export interface Pagination {
  page: number;
  perPage: number;
  pageCount: number;
  pageItemsCount: number;
  totalCount: number;
}

export interface ListTopicsResponse {
  data: KafkaTopic[];
  pagination: Pagination;
}

export interface TopicPartitionDetail {
  id: number;
  leader: KafkaNode;
  replicas: KafkaNode[];
  isr: KafkaNode[];
  offline: KafkaNode[];
}

export interface TopicConfig {
  name: string;
  value: string;
  source: string;
  readOnly: boolean;
  sensitive: boolean;
}

export interface DescribeTopicResponse {
  name: string;
  internal: boolean;
  partitions: TopicPartitionDetail[];
  configs: TopicConfig[];
}

export interface BrokerLogDirEntry {
  path: string;
  error?: string;
  topics: number;
  partitions: number;
  size: number;
}

export interface DescribeBrokerResponse {
  id: number;
  host: string;
  port: number;
  rack?: string;
  isController: boolean;
  leaderPartitions: number;
  replicaPartitions: number;
  logDirSize?: number;
  logDirEntries: BrokerLogDirEntry[];
  configs: TopicConfig[];
}
