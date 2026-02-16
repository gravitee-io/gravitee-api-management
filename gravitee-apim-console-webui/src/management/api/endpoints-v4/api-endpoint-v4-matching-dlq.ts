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

import { ApiV4, Entrypoint } from '../../../entities/management-api-v2';

export const getMatchingDlqEntrypointsForGroup = (api: ApiV4, currentGroup: string): Entrypoint[] => {
  if (!api) {
    return [];
  }
  const endpointNames = api.endpointGroups.find(group => group.name === currentGroup).endpoints.map(endpoint => endpoint.name);
  return getEntrypointsWithDlq(api).filter(
    entrypoint => entrypoint.dlq.endpoint === currentGroup || endpointNames.includes(entrypoint.dlq.endpoint),
  );
};

export const getMatchingDlqEntrypoints = (api: ApiV4, currentEndpoint: string): Entrypoint[] => {
  if (!api) {
    return [];
  }
  return getEntrypointsWithDlq(api).filter(entrypoint => entrypoint.dlq.endpoint === currentEndpoint);
};

export const updateDlqEntrypoint = (api: ApiV4, dlqEntrypoints: Entrypoint[], newName: string): void => {
  api.listeners
    .flatMap(listener => listener.entrypoints)
    .filter(entrypoint => dlqEntrypoints.map(dlqEntrypoint => dlqEntrypoint.type).includes(entrypoint.type))
    .forEach(dlqEntrypoint => (dlqEntrypoint.dlq.endpoint = newName));
};

export const disableDlqEntrypoint = (api: ApiV4, dlqEntrypoints: Entrypoint[]): void => {
  api.listeners
    .flatMap(listener => listener.entrypoints)
    .filter(entrypoint => dlqEntrypoints.map(dlqEntrypoint => dlqEntrypoint.type).includes(entrypoint.type))
    .forEach(dlqEntrypoint => (dlqEntrypoint.dlq = null));
};

function getEntrypointsWithDlq(api: ApiV4): Entrypoint[] {
  const entrypoints = api.listeners?.flatMap(listeners => listeners.entrypoints);
  if (!entrypoints) {
    return [];
  }

  return entrypoints.filter(entrypoint => !!entrypoint.dlq);
}
