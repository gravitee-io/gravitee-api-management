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

// ── Type-safe helpers ──

function asString(v: unknown, fallback: string): string {
  return typeof v === 'string' ? v : fallback;
}

function asBoolean(v: unknown, fallback: boolean): boolean {
  return typeof v === 'boolean' ? v : fallback;
}

function asNumber(v: unknown, fallback: number): number {
  return typeof v === 'number' ? v : fallback;
}

function asRecord(v: unknown): Record<string, unknown> {
  return typeof v === 'object' && v !== null ? (v as Record<string, unknown>) : {};
}

function parseConfig(config: unknown): Record<string, unknown> {
  if (!config) return {};
  if (typeof config === 'string') {
    try {
      const parsed: unknown = JSON.parse(config);
      return typeof parsed === 'object' && parsed !== null ? (parsed as Record<string, unknown>) : {};
    } catch {
      return {};
    }
  }
  return typeof config === 'object' && config !== null ? (config as Record<string, unknown>) : {};
}
