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
import type { ApplicationListItem } from '../types/application';

const TYPE_LABELS: Record<string, string> = {
    SIMPLE: 'Service',
    BROWSER: 'SPA',
    WEB: 'Web',
    NATIVE: 'Mobile',
    BACKEND_TO_BACKEND: 'Backend',
    AI_AGENT: 'AI Agent',
};

export function formatApplicationTypeLabel(application: Pick<ApplicationListItem, 'type' | 'settings'>): string {
    const settingsType = application.settings?.app?.type;
    if (settingsType) {
        return TYPE_LABELS[settingsType] ?? toTitleLabel(settingsType);
    }
    if (!application.type) {
        return '—';
    }
    return TYPE_LABELS[application.type] ?? toTitleLabel(application.type);
}

function toTitleLabel(value: string): string {
    return value
        .toLowerCase()
        .split('_')
        .map(part => part.charAt(0).toUpperCase() + part.slice(1))
        .join(' ');
}
