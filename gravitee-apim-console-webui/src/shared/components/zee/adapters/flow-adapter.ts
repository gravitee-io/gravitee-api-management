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

interface FlowStep {
  name: string;
  policy: string;
  enabled: boolean;
  description?: string;
  condition?: string;
  configuration: Record<string, unknown>;
  messageCondition?: string;
}

interface FlowSelector {
  type: string;
  path?: string;
  pathOperator?: string;
  methods?: string[];
  channel?: string;
  channelOperator?: string;
  operations?: string[];
  entrypoints?: string[];
  condition?: string;
}

interface FlowPayload {
  name: string;
  enabled: boolean;
  selectors: FlowSelector[];
  request: FlowStep[];
  response: FlowStep[];
  subscribe: FlowStep[];
  publish: FlowStep[];
  tags: string[];
}

export const FLOW_ADAPTER: ZeeResourceAdapter<FlowPayload> = {
  previewLabel: 'Generated Flow',
  transform: (generated: unknown): FlowPayload => {
    const g = generated as Record<string, unknown>;
    return {
      name: asString(g.name, ''),
      enabled: asBoolean(g.enabled, true),
      selectors: asArray(g.selectors).map(mapSelector),
      request: asArray(g.request).map(mapStep),
      response: asArray(g.response).map(mapStep),
      subscribe: asArray(g.subscribe).map(mapStep),
      publish: asArray(g.publish).map(mapStep),
      tags: asStringArray(g.tags),
    };
  },
};

function mapStep(raw: unknown): FlowStep {
  const s = raw as Record<string, unknown>;
  return {
    name: asString(s.name, ''),
    policy: asString(s.policy, ''),
    enabled: asBoolean(s.enabled, true),
    description: asOptionalString(s.description),
    condition: asOptionalString(s.condition),
    configuration: parseConfig(s.configuration),
    messageCondition: asOptionalString(s.messageCondition),
  };
}

function mapSelector(raw: unknown): FlowSelector {
  const sel = raw as Record<string, unknown>;
  const type = asString(sel.type, '').toUpperCase();
  switch (type) {
    case 'HTTP':
      return { type: 'HTTP', path: asOptionalString(sel.path), pathOperator: asOptionalString(sel.pathOperator), methods: asStringArray(sel.methods) };
    case 'CHANNEL':
      return {
        type: 'CHANNEL',
        channel: asOptionalString(sel.channel),
        channelOperator: asOptionalString(sel.channelOperator),
        operations: asStringArray(sel.operations),
        entrypoints: asStringArray(sel.entrypoints),
      };
    case 'CONDITION':
      return { type: 'CONDITION', condition: asOptionalString(sel.condition) };
    default:
      return { type };
  }
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

function asStringArray(v: unknown): string[] {
  return Array.isArray(v) ? v.filter((item): item is string => typeof item === 'string') : [];
}

function asArray(v: unknown): unknown[] {
  return Array.isArray(v) ? v : [];
}

function parseConfig(config: unknown): Record<string, unknown> {
  if (!config) return {};
  if (typeof config === 'string') {
    try {
      const parsed: unknown = JSON.parse(config);
      return typeof parsed === 'object' && parsed !== null ? (parsed as Record<string, unknown>) : {};
    } catch {
      console.warn('Zee FLOW_ADAPTER: Failed to parse configuration JSON', config);
      return {};
    }
  }
  return typeof config === 'object' && config !== null ? (config as Record<string, unknown>) : {};
}
