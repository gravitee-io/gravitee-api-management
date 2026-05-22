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
import { NativeConnectionStatus } from '../../../../entities/management-api-v2';

// Used when the URL has no `period` or it's unrecognised.
export const DEFAULT_NATIVE_LOGS_PERIOD = '1d';

export interface NativeConnectionStatusMeta {
  label: string;
  // gio-badge-* utility class — colors the badge in both the table cell and the summary card.
  badgeClass: string;
  icon: string;
  isErrored: boolean;
}

// Record forces compile-time exhaustiveness — adding a new status to the union without an entry here is a build error.
export const NATIVE_STATUS_META: Record<NativeConnectionStatus, NativeConnectionStatusMeta> = {
  CONNECTED: { label: 'Connected', badgeClass: 'gio-badge-success', icon: 'gio:check-circled-outline', isErrored: false },
  SESSION_ERROR: { label: 'Disconnected', badgeClass: 'gio-badge-warning', icon: 'gio:alert-circle', isErrored: true },
  CONNECTION_ERROR: { label: 'Failed', badgeClass: 'gio-badge-error', icon: 'gio:error', isErrored: true },
  INTERNAL_ERROR: { label: 'Unknown', badgeClass: 'gio-badge-accent', icon: 'gio:shield-alert', isErrored: true },
};

export const NATIVE_CONNECTION_STATUSES: { value: NativeConnectionStatus; label: string }[] = (
  Object.entries(NATIVE_STATUS_META) as [NativeConnectionStatus, NativeConnectionStatusMeta][]
).map(([value, meta]) => ({ value, label: meta.label }));

// Derived from the meta table to stay in sync: adding a new status to NATIVE_STATUS_META
// auto-appears in the summary widget at the position it was inserted at (healthy → degraded → fatal).
export const NATIVE_SUMMARY_STATUSES = Object.keys(NATIVE_STATUS_META) as NativeConnectionStatus[];

export const isNativeConnectionErrored = (status: NativeConnectionStatus | undefined): boolean =>
  status != null && NATIVE_STATUS_META[status].isErrored;
