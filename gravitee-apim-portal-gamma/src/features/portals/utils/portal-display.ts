/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import type { DeveloperPortal } from '../types';

export type PortalPublishStatus = 'Published' | 'Draft';

export function getPortalPublishStatus(portal: DeveloperPortal): PortalPublishStatus {
    return portal.portalUrl?.trim() ? 'Published' : 'Draft';
}

export function getPortalCustomDomain(portal: DeveloperPortal): string {
    const url = portal.portalUrl?.trim();
    if (!url) {
        return '-';
    }

    try {
        return new URL(url).hostname;
    } catch {
        return url.replace(/^https?:\/\//, '').split('/')[0] || '-';
    }
}

const RELATIVE_TIME = new Intl.RelativeTimeFormat(undefined, { numeric: 'auto' });

export function formatRelativeUpdatedAt(isoDate: string, now = Date.now()): string {
    const then = new Date(isoDate).getTime();
    if (Number.isNaN(then)) {
        return '-';
    }

    const diffSeconds = Math.round((then - now) / 1000);
    const absSeconds = Math.abs(diffSeconds);

    if (absSeconds < 60) {
        return RELATIVE_TIME.format(diffSeconds, 'second');
    }
    if (absSeconds < 3600) {
        return RELATIVE_TIME.format(Math.round(diffSeconds / 60), 'minute');
    }
    if (absSeconds < 86_400) {
        return RELATIVE_TIME.format(Math.round(diffSeconds / 3600), 'hour');
    }
    if (absSeconds < 2_592_000) {
        return RELATIVE_TIME.format(Math.round(diffSeconds / 86_400), 'day');
    }
    if (absSeconds < 31_536_000) {
        return RELATIVE_TIME.format(Math.round(diffSeconds / 2_592_000), 'month');
    }
    return RELATIVE_TIME.format(Math.round(diffSeconds / 31_536_000), 'year');
}
