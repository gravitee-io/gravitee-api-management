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
export interface MonitoringData {
  cpu: {
    percent_use: number;
    load_average: any;
  };
  process: {
    cpu_percent: number;
    open_file_descriptors: number;
    max_file_descriptors: number;
  };
  jvm: {
    timestamp: Date;
    uptime_in_millis: number;
    heap_used_in_bytes: number;
    heap_used_percent: number;
    heap_committed_in_bytes: number;
    heap_max_in_bytes: number;
    non_heap_used_in_bytes: number;
    non_heap_committed_in_bytes: number;
    young_pool_used_in_bytes: number;
    young_pool_max_in_bytes: number;
    young_pool_peak_used_in_bytes: number;
    young_pool_peak_max_in_bytes: number;
    survivor_pool_used_in_bytes: number;
    survivor_pool_max_in_bytes: number;
    survivor_pool_peak_used_in_bytes: number;
    survivor_pool_peak_max_in_bytes: number;
    old_pool_used_in_bytes: number;
    old_pool_max_in_bytes: number;
    old_pool_peak_used_in_bytes: number;
    old_pool_peak_max_in_bytes: number;
  };
  thread: {
    count: number;
    peak_count: number;
  };
  gc: {
    young_collection_count: number;
    young_collection_time_in_millis: number;
    old_collection_count: number;
    old_collection_time_in_millis: number;
  };
}
