/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import type { ApiDetailDto, DuplicateFilteredField, HttpListener } from '../types';

export type DuplicateEntryMode = 'contextPath' | 'host' | 'none';

export const DUPLICATE_INCLUDE_OPTIONS: { id: DuplicateFilteredField; label: string }[] = [
    { id: 'GROUPS', label: 'Groups' },
    { id: 'MEMBERS', label: 'Members' },
    { id: 'PAGES', label: 'Pages' },
    { id: 'PLANS', label: 'Plans' },
];

export function getDuplicateEntryMode(api: ApiDetailDto | null): DuplicateEntryMode {
    if (!api) return 'none';
    if (api.listeners?.some(l => (l as { type?: string }).type === 'TCP')) {
        return 'host';
    }
    if (api.listeners?.some(l => (l as { type?: string }).type === 'HTTP')) {
        return 'contextPath';
    }
    return 'none';
}

export function extractContextPathPlaceholder(api: ApiDetailDto | null): string {
    if (!api) return '';
    const httpListener = api.listeners?.find(l => (l as { type?: string }).type === 'HTTP') as HttpListener | undefined;
    if (!httpListener) return '';

    const firstPath = httpListener.paths?.[0];
    if (firstPath) {
        return firstPath.path;
    }

    const firstHost = httpListener.hosts?.[0];
    if (firstHost) {
        return `${firstHost.host ?? ''}${firstHost.path}`;
    }

    return '';
}

export function extractHostPlaceholder(api: ApiDetailDto | null): string {
    if (!api) return '';
    const tcpListener = api.listeners?.find(l => (l as { type?: string }).type === 'TCP') as { hosts?: { host?: string }[] } | undefined;
    return tcpListener?.hosts?.[0]?.host ?? '';
}

export function buildDuplicateFilteredFields(include: Record<DuplicateFilteredField, boolean>): DuplicateFilteredField[] {
    return DUPLICATE_INCLUDE_OPTIONS.filter(option => !include[option.id]).map(option => option.id);
}
