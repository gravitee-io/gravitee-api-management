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

interface PlanSecurity {
  type: string;
  configuration: Record<string, unknown>;
}

interface PlanFlowStep {
  name: string;
  policy: string;
  enabled: boolean;
  description?: string;
  condition?: string;
  configuration: Record<string, unknown>;
}

interface PlanFlow {
  name: string;
  enabled: boolean;
  selectors: unknown[];
  request: PlanFlowStep[];
  response: PlanFlowStep[];
  subscribe: PlanFlowStep[];
  publish: PlanFlowStep[];
  tags: string[];
}

interface PlanPayload {
  name: string;
  description: string;
  validation: string;
  mode: string;
  security: PlanSecurity;
  characteristics: string[];
  commentRequired: boolean;
  commentMessage: string;
  generalConditions: string;
  excludedGroups: string[];
  flows: PlanFlow[];
  selectionRule: string;
  tags: string[];
  status: string;
  type: string;
  order: number;
}

export const PLAN_ADAPTER: ZeeResourceAdapter<PlanPayload> = {
  previewLabel: 'Generated Plan',
  transform: (generated: unknown): PlanPayload => {
    const g = generated as Record<string, unknown>;
    return {
      name: asString(g.name, ''),
      description: asString(g.description, ''),
      validation: asString(g.validation, 'AUTO'),
      mode: asString(g.mode, 'STANDARD'),
      security: mapSecurity(g.security),
      characteristics: asStringArray(g.characteristics),
      commentRequired: asBoolean(g.commentRequired, false),
      commentMessage: asString(g.commentMessage, ''),
      generalConditions: asString(g.generalConditions, ''),
      excludedGroups: asStringArray(g.excludedGroups),
      flows: asArray(g.flows).map(mapFlow),
      selectionRule: asString(g.selectionRule, ''),
      tags: asStringArray(g.tags),
      status: asString(g.status, 'STAGING'),
      type: asString(g.type, 'API'),
      order: asNumber(g.order, 0),
    };
  },
};

function mapSecurity(raw: unknown): PlanSecurity {
  if (!raw || typeof raw !== 'object') {
    return { type: 'KEY_LESS', configuration: {} };
  }
  const s = raw as Record<string, unknown>;
  return {
    type: asString(s.type, 'KEY_LESS'),
    configuration: parseConfig(s.configuration),
  };
}

function mapFlow(raw: unknown): PlanFlow {
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

function mapStep(raw: unknown): PlanFlowStep {
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
