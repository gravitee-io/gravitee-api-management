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

import { KAFKA_DOMAIN_PLACEHOLDER, isValidKafkaDomain } from './entrypointForm';
import type { EntrypointPortalSettings } from '../types/entrypoint';

/** Classic `portValidator` range for portal entrypoint defaults (TCP / Kafka). */
export const CONFIG_PORT_MIN = 1025;
export const CONFIG_PORT_MAX = 65535;

export type EntrypointConfigFieldKey = 'entrypoint' | 'tcpPort' | 'kafkaDomain' | 'kafkaPort';

export interface EntrypointConfigFormValues {
    entrypoint: string;
    tcpPort: string;
    kafkaDomain: string;
    kafkaPort: string;
}

export const EMPTY_ENTRYPOINT_CONFIG_FORM: EntrypointConfigFormValues = {
    entrypoint: '',
    tcpPort: '',
    kafkaDomain: '',
    kafkaPort: '',
};

const READONLY_PROPERTY_BY_FIELD: Record<EntrypointConfigFieldKey, string> = {
    entrypoint: 'portal.entrypoint',
    tcpPort: 'portal.tcpPort',
    kafkaDomain: 'portal.kafkaDomain',
    kafkaPort: 'portal.kafkaPort',
};

export function isValidConfigPort(raw: string): boolean {
    if (!raw.trim()) return false;
    if (!/^\d+$/.test(raw.trim())) return false;
    const port = Number(raw);
    return port >= CONFIG_PORT_MIN && port <= CONFIG_PORT_MAX;
}

export function toEntrypointConfigFormValues(settings: EntrypointPortalSettings): EntrypointConfigFormValues {
    const portal = settings.portal ?? {};
    return {
        entrypoint: portal.entrypoint ?? '',
        tcpPort: portal.tcpPort !== undefined && portal.tcpPort !== null ? String(portal.tcpPort) : '',
        kafkaDomain: portal.kafkaDomain ?? '',
        kafkaPort: portal.kafkaPort !== undefined && portal.kafkaPort !== null ? String(portal.kafkaPort) : '',
    };
}

export function isEntrypointConfigFieldReadonly(settings: EntrypointPortalSettings | undefined, field: EntrypointConfigFieldKey): boolean {
    const property = READONLY_PROPERTY_BY_FIELD[field];
    return settings?.metadata?.readonly?.some(key => key === property) ?? false;
}

export function isEntrypointConfigFormValid(form: EntrypointConfigFormValues): boolean {
    return isValidConfigPort(form.tcpPort) && isValidKafkaDomain(form.kafkaDomain) && isValidConfigPort(form.kafkaPort);
}

export function isEntrypointConfigFormDirty(current: EntrypointConfigFormValues, saved: EntrypointConfigFormValues): boolean {
    return (
        current.entrypoint !== saved.entrypoint ||
        current.tcpPort !== saved.tcpPort ||
        current.kafkaDomain !== saved.kafkaDomain ||
        current.kafkaPort !== saved.kafkaPort
    );
}

/** Merges edited portal defaults into the full settings document (Classic save shape). */
export function buildEntrypointPortalSettingsPayload(
    current: EntrypointPortalSettings,
    form: EntrypointConfigFormValues,
): EntrypointPortalSettings {
    return {
        ...current,
        portal: {
            ...(current.portal ?? {}),
            entrypoint: form.entrypoint.trim(),
            tcpPort: Number(form.tcpPort),
            kafkaDomain: form.kafkaDomain.trim(),
            kafkaPort: Number(form.kafkaPort),
        },
    };
}

export { KAFKA_DOMAIN_PLACEHOLDER };
