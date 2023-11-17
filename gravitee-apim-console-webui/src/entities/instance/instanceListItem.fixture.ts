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
import { SearchResult } from './searchResult';
import { InstanceListItem } from './instanceListItem';

export function fakeInstanceListItem(attributes?: Partial<InstanceListItem>): InstanceListItem {
  const defaultValue: InstanceListItem = {
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

export function fakeSearchResult(attributes?: Partial<SearchResult>): SearchResult {
  const defaultValue: SearchResult = {
    content: [fakeInstanceListItem()],
    pageElements: 1,
    pageNumber: 0,
    totalElements: 1,
  };

  return {
    ...defaultValue,
    ...attributes,
  };
}
