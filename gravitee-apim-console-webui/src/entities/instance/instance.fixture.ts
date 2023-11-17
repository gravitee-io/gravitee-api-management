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
import { Instance } from './instance';
import { MonitoringData } from './monitoringData';

export function fakeInstance(attributes?: Partial<Instance>): Instance {
  const defaultValue: Instance = {
    event: 'a8da6459-dc59-4cdb-9a64-59dc596cdb74',
    id: '5bc17c57-b350-460d-817c-57b350060db3',
    hostname: 'apim-master-v3-apim3-gateway-6575b8ccf7-m4s6j',
    ip: '0.0.0.0',
    port: '8082',
    version: '3.20.0-SNAPSHOT (build: 174998) revision#a67b37a366',
    state: 'STARTED',
    started_at: 1667812198374,
    last_heartbeat_at: 1667813521610,
    operating_system_name: 'Linux',
  };

  return {
    ...defaultValue,
    ...attributes,
  };
}

export function fakeMonitoringData(attributes?: Partial<MonitoringData>): MonitoringData {
  const defaultValue: MonitoringData = {
    cpu: {
      percent_use: 9,
      load_average: {
        '1m': 1.08,
        '5m': 0.76,
        '15m': 0.77,
      },
    },
    process: {
      cpu_percent: 18,
      open_file_descriptors: 444,
      max_file_descriptors: 1048576,
    },
    jvm: {
      timestamp: new Date(1667814960307),
      uptime_in_millis: 2814465,
      heap_used_in_bytes: 84563320,
      heap_used_percent: 32,
      heap_committed_in_bytes: 259522560,
      heap_max_in_bytes: 259522560,
      non_heap_used_in_bytes: 173936576,
      non_heap_committed_in_bytes: 180551680,
      young_pool_used_in_bytes: 17375680,
      young_pool_max_in_bytes: 71630848,
      young_pool_peak_used_in_bytes: 71630848,
      young_pool_peak_max_in_bytes: 71630848,
      survivor_pool_used_in_bytes: 414248,
      survivor_pool_max_in_bytes: 8912896,
      survivor_pool_peak_used_in_bytes: 8912896,
      survivor_pool_peak_max_in_bytes: 8912896,
      old_pool_used_in_bytes: 66840632,
      old_pool_max_in_bytes: 178978816,
      old_pool_peak_used_in_bytes: 66840632,
      old_pool_peak_max_in_bytes: 178978816,
    },
    thread: {
      count: 119,
      peak_count: 124,
    },
    gc: {
      young_collection_count: 85,
      young_collection_time_in_millis: 1253,
      old_collection_count: 4,
      old_collection_time_in_millis: 971,
    },
  };

  return {
    ...defaultValue,
    ...attributes,
  };
}
