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

import { ZeeResourceAdapter } from '../zee.model';
import { asBoolean, asNumber, asRecord, asString, parseConfig } from './helpers';

interface EndpointPayload {
  name: string;
  type: string;
  weight: number;
  inheritConfiguration: boolean;
  secondary: boolean;
  configuration: Record<string, unknown>;
  sharedConfigurationOverride: Record<string, unknown>;
  services: Record<string, unknown>;
}

export const ENDPOINT_ADAPTER: ZeeResourceAdapter<EndpointPayload> = {
  previewLabel: 'Generated Endpoint',
  transform: (generated: unknown): EndpointPayload => {
    const g = generated as Record<string, unknown>;
    return {
      name: asString(g.name, ''),
      type: asString(g.type, 'http-proxy'),
      weight: asNumber(g.weight, 1),
      inheritConfiguration: asBoolean(g.inheritConfiguration, true),
      secondary: asBoolean(g.secondary, false),
      configuration: parseConfig(g.configuration),
      sharedConfigurationOverride: parseConfig(g.sharedConfigurationOverride),
      services: asRecord(g.services),
    };
  },
};
