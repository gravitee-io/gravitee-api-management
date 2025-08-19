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
export interface InstanceListItem {
  event: string;
  id: string;
  started_at: number;
  last_heartbeat_at: number;
  stopped_at?: number;
  hostname: string;
  ip: string;
  port: string;
  version: string | null;
  tags?: string[];
  tenant?: string;
  state: 'STARTED' | 'STOPPED' | 'UNKNOWN';
  operating_system_name?: string;
}
