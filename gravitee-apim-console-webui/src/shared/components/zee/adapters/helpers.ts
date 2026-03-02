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

/**
 * Shared type-safe coercion helpers for Zee resource adapters.
 *
 * Every adapter transforms untyped LLM output into a known payload shape.
 * These helpers provide safe narrowing with sensible fallbacks so each
 * adapter only needs to worry about its domain-specific mapping logic.
 */

export function asString(v: unknown, fallback: string): string {
  return typeof v === 'string' ? v : fallback;
}

export function asOptionalString(v: unknown): string | undefined {
  return typeof v === 'string' ? v : undefined;
}

export function asBoolean(v: unknown, fallback: boolean): boolean {
  return typeof v === 'boolean' ? v : fallback;
}

export function asNumber(v: unknown, fallback: number): number {
  return typeof v === 'number' ? v : fallback;
}

export function asStringArray(v: unknown): string[] {
  return Array.isArray(v) ? v.filter((item): item is string => typeof item === 'string') : [];
}

export function asArray(v: unknown): unknown[] {
  return Array.isArray(v) ? v : [];
}

export function asRecord(v: unknown): Record<string, unknown> {
  return typeof v === 'object' && v !== null ? (v as Record<string, unknown>) : {};
}

export function parseConfig(config: unknown): Record<string, unknown> {
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
