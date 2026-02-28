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

interface ApiFlowStep {
  name: string;
  policy: string;
  enabled: boolean;
  description?: string;
  condition?: string;
  configuration: Record<string, unknown>;
}

interface ApiFlow {
  name: string;
  enabled: boolean;
  selectors: unknown[];
  request: ApiFlowStep[];
  response: ApiFlowStep[];
  subscribe: ApiFlowStep[];
  publish: ApiFlowStep[];
  tags: string[];
}

interface ApiEntrypoint {
  type: string;
  configuration: Record<string, unknown>;
  dlq: { endpoint: string } | null;
}

interface ApiListener {
  type: string;
  entrypoints: ApiEntrypoint[];
  paths: unknown[];
  servers: unknown[];
  cors?: unknown | null;
}

interface ApiEndpoint {
  name: string;
  type: string;
  weight: number;
  inheritConfiguration: boolean;
  secondary: boolean;
  configuration: Record<string, unknown>;
  services: Record<string, unknown>;
}

interface ApiEndpointGroup {
  name: string;
  type: string;
  loadBalancer: { type: string };
  endpoints: ApiEndpoint[];
  sharedConfiguration: Record<string, unknown>;
  services: Record<string, unknown>;
}

interface ApiProperty {
  key: string;
  value: string;
}

interface ApiPayload {
  name: string;
  apiVersion: string;
  description: string;
  type: string;
  listeners: ApiListener[];
  endpointGroups: ApiEndpointGroup[];
  flows: ApiFlow[];
  properties: ApiProperty[];
  tags: string[];
}

export const API_ADAPTER: ZeeResourceAdapter<ApiPayload> = {
  previewLabel: 'Generated API',
  transform: (generated: unknown): ApiPayload => {
    const g = generated as Record<string, unknown>;
    return {
      name: asString(g.name, ''),
      apiVersion: asString(g.apiVersion ?? g.version, '1.0.0'),
      description: asString(g.description, ''),
      type: asString(g.type, 'PROXY'),
      listeners: asArray(g.listeners).map(mapListener),
      endpointGroups: asArray(g.endpointGroups).map(mapEndpointGroup),
      flows: asArray(g.flows).map(mapFlow),
      properties: asArray(g.properties).map(mapProperty),
      tags: asStringArray(g.tags),
    };
  },
};

function mapListener(raw: unknown): ApiListener {
  const l = raw as Record<string, unknown>;
  const type = asString(l.type, '');
  return {
    type,
    entrypoints: asArray(l.entrypoints).map(mapEntrypoint),
    paths: asArray(l.paths),
    servers: asArray(l.servers),
    ...(type === 'HTTP' ? { cors: l.cors ?? null } : {}),
  };
}

function mapEntrypoint(raw: unknown): ApiEntrypoint {
  const e = raw as Record<string, unknown>;
  return {
    type: asString(e.type, ''),
    configuration: parseConfig(e.configuration),
    dlq: mapDlq(e.dlq),
  };
}

function mapDlq(raw: unknown): { endpoint: string } | null {
  if (!raw || typeof raw !== 'object') return null;
  const d = raw as Record<string, unknown>;
  const endpoint = d.endpoint;
  if (typeof endpoint !== 'string') return null;
  return { endpoint };
}

function mapEndpointGroup(raw: unknown): ApiEndpointGroup {
  const g = raw as Record<string, unknown>;
  return {
    name: asString(g.name, ''),
    type: asString(g.type, 'http-proxy'),
    loadBalancer: mapLoadBalancer(g.loadBalancer),
    endpoints: asArray(g.endpoints).map(mapEndpoint),
    sharedConfiguration: parseConfig(g.sharedConfiguration),
    services: asRecord(g.services),
  };
}

function mapLoadBalancer(raw: unknown): { type: string } {
  if (!raw || typeof raw !== 'object') return { type: 'ROUND_ROBIN' };
  const lb = raw as Record<string, unknown>;
  return { type: asString(lb.type, 'ROUND_ROBIN') };
}

function mapEndpoint(raw: unknown): ApiEndpoint {
  const e = raw as Record<string, unknown>;
  return {
    name: asString(e.name, ''),
    type: asString(e.type, 'http-proxy'),
    weight: asNumber(e.weight, 1),
    inheritConfiguration: asBoolean(e.inheritConfiguration, true),
    secondary: asBoolean(e.secondary, false),
    configuration: parseConfig(e.configuration),
    services: asRecord(e.services),
  };
}

function mapFlow(raw: unknown): ApiFlow {
  const f = raw as Record<string, unknown>;
  return {
    name: asString(f.name, ''),
    enabled: asBoolean(f.enabled, true),
    selectors: asArray(f.selectors),
    request: asArray(f.request).map(mapStep),
    response: asArray(f.response).map(mapStep),
    subscribe: asArray(f.subscribe).map(mapStep),
    publish: asArray(f.publish).map(mapStep),
    tags: asStringArray(f.tags),
  };
}

function mapStep(raw: unknown): ApiFlowStep {
  const s = raw as Record<string, unknown>;
  return {
    name: asString(s.name, ''),
    policy: asString(s.policy, ''),
    enabled: asBoolean(s.enabled, true),
    description: asOptionalString(s.description),
    condition: asOptionalString(s.condition),
    configuration: parseConfig(s.configuration),
  };
}

function mapProperty(raw: unknown): ApiProperty {
  const p = raw as Record<string, unknown>;
  return {
    key: asString(p.key, ''),
    value: asString(p.value, ''),
  };
}

// ── Type-safe helpers ──

function asString(v: unknown, fallback: string): string {
  return typeof v === 'string' ? v : fallback;
}

function asOptionalString(v: unknown): string | undefined {
  return typeof v === 'string' ? v : undefined;
}

function asBoolean(v: unknown, fallback: boolean): boolean {
  return typeof v === 'boolean' ? v : fallback;
}

function asNumber(v: unknown, fallback: number): number {
  return typeof v === 'number' ? v : fallback;
}

function asStringArray(v: unknown): string[] {
  return Array.isArray(v) ? v.filter((item): item is string => typeof item === 'string') : [];
}

function asArray(v: unknown): unknown[] {
  return Array.isArray(v) ? v : [];
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
