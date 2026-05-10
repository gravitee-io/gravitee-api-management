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

export interface NativeConnectionStatusMeta {
  label: string;
  badgeClass: string;
}

// Record forces compile-time exhaustiveness — adding a new status to the union without an entry here is a build error.
export const NATIVE_STATUS_META: Record<NativeConnectionStatus, NativeConnectionStatusMeta> = {
  CONNECTED: { label: 'Connected', badgeClass: 'gio-badge-success' },
  CONNECTION_ERROR: { label: 'Failed', badgeClass: 'gio-badge-warning' },
  SESSION_ERROR: { label: 'Disconnected', badgeClass: 'gio-badge-warning' },
  INTERNAL_ERROR: { label: 'Unknown', badgeClass: 'gio-badge-error' },
};

export const NATIVE_CONNECTION_STATUSES: { value: NativeConnectionStatus; label: string }[] = (
  Object.entries(NATIVE_STATUS_META) as [NativeConnectionStatus, NativeConnectionStatusMeta][]
).map(([value, meta]) => ({ value, label: meta.label }));
