/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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

/** Visual identity of one observability signal, as shown next to a filter. */
export interface SignalEntry {
  /** Wire value, as published by the filter catalog. Never shown to users. */
  readonly id: string;
  /**
   * What users call the screen this filter works on, which is not always the signal's own name — the console
   * has a separate "Analytics" menu, so calling the dashboards signal "Analytics" would send someone looking
   * in the wrong place.
   */
  readonly label: string;
  /**
   * Gravitee icon name, passed to `mat-icon`'s `svgIcon`. The `gio:` namespace is registered by the host
   * application, as it already is for the other icons this library renders.
   */
  readonly icon: string;
}

/**
 * Single source of truth for how a signal is shown.
 *
 * Mirrors the registry the Gamma observability UI uses, so the same filter carries the same identity in both
 * products. Keep the ids aligned with the `Signal` enum served by the Management API.
 */
export const SIGNAL_REGISTRY: readonly SignalEntry[] = [
  { id: 'LOGS', label: 'Logs', icon: 'gio:page' },
  { id: 'ANALYTICS', label: 'Dashboard', icon: 'gio:stat-up' },
  { id: 'TRACES', label: 'Tracing', icon: 'gio:list' },
];

const REGISTRY_BY_ID = new Map(SIGNAL_REGISTRY.map(entry => [entry.id, entry]));

export function getSignal(id: string): SignalEntry | undefined {
  return REGISTRY_BY_ID.get(id.toUpperCase());
}
