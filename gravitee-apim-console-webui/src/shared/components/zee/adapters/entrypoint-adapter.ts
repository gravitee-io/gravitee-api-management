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

interface EntrypointDlq {
  endpoint: string;
}

interface EntrypointPayload {
  type: string;
  configuration: Record<string, unknown>;
  dlq: EntrypointDlq | null;
}

export const ENTRYPOINT_ADAPTER: ZeeResourceAdapter<EntrypointPayload> = {
  previewLabel: 'Generated Entrypoint',
  transform: (generated: unknown): EntrypointPayload => {
    const g = generated as Record<string, unknown>;
    return {
      type: asString(g.type, 'http-proxy'),
      configuration: parseConfig(g.configuration),
      dlq: mapDlq(g.dlq),
    };
  },
};

function mapDlq(raw: unknown): EntrypointDlq | null {
  if (!raw || typeof raw !== 'object') return null;
  const d = raw as Record<string, unknown>;
  const endpoint = d.endpoint;
  if (typeof endpoint !== 'string') return null;
  return { endpoint };
}

// ── Type-safe helpers ──

function asString(v: unknown, fallback: string): string {
  return typeof v === 'string' ? v : fallback;
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
