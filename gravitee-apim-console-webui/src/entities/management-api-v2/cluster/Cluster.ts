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
export type ClusterType = 'KAFKA_CLUSTER_STANDALONE' | 'KAFKA_CLUSTER';
export type ClusterLifecycleState = 'UNDEPLOYED' | 'DEPLOYED' | 'PENDING';

export interface Cluster {
  id: string;
  crossId: string;
  type: ClusterType;
  name: string;
  description?: string;
  configuration: KafkaClusterStandaloneConfiguration | KafkaClusterConfiguration;
  updatedAt: Date;
  createdAt: Date;
  groups: string[];
  lifecycleState?: ClusterLifecycleState;
  deployedAt?: Date;
  version?: number;
}

export interface KafkaClusterStandaloneConfiguration {
  bootstrapServers?: string;
  security?: unknown;
}

export interface KafkaClusterConnection {
  crossId?: string;
  name: string;
  bootstrapServers?: string;
  security?: unknown;
}

export interface KafkaClusterConfiguration {
  connections?: KafkaClusterConnection[];
}
