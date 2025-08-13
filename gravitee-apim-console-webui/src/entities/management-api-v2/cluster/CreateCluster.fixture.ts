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
import { CreateCluster } from './CreateCluster';

export function fakeCreateCluster(overrides: Partial<CreateCluster> = {}): CreateCluster {
  return {
    name: 'New Cluster Name',
    description: 'A new test cluster',
    configuration: {
      bootstrapServers: 'kafka.example.com:9092',
      security: 'none',
    },
    ...overrides,
  };
}
