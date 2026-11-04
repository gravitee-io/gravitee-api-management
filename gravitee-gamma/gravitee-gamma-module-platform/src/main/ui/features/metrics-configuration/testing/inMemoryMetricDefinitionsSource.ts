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
import {
    type AdditionalMetricValueType,
    type DeclareMetricDefinitionRequest,
    deriveAdditionalMetricLabel,
    type MetricDefinitionEntry,
    type MetricDefinitionFilterType,
    type MetricDefinitionRecord,
    type MetricDefinitionsResponse,
    type MetricDefinitionsSource,
    ObservabilityHttpError,
    type SaveMetricDefinitionRequest,
    type SavedMetricDefinition,
} from '@gravitee/gamma-lib-observability';

const KEY_PREFIX = 'additional-metrics.';
const SAVED_AT = '2026-11-04T10:00:00Z';

const VALUE_TYPES: Readonly<Record<string, { valueType: AdditionalMetricValueType; type?: MetricDefinitionFilterType }>> = {
    keyword: { valueType: 'KEYWORD', type: 'keyword' },
    long: { valueType: 'LONG', type: 'number' },
    int: { valueType: 'INT', type: 'number' },
    double: { valueType: 'DOUBLE', type: 'number' },
    bool: { valueType: 'BOOL', type: 'boolean' },
    string: { valueType: 'STRING' },
    json: { valueType: 'JSON' },
};

function rawKeyOf(name: string): string {
    return name.startsWith(KEY_PREFIX) ? name.slice(KEY_PREFIX.length) : name;
}

function typeOf(rawKey: string): { valueType: AdditionalMetricValueType; type?: MetricDefinitionFilterType } {
    return VALUE_TYPES[rawKey.split('_')[0] ?? ''] ?? { valueType: 'KEYWORD', type: 'keyword' };
}

function recordOf(entry: MetricDefinitionEntry): MetricDefinitionRecord {
    return {
        name: entry.name,
        rawKey: entry.rawKey,
        valueType: entry.valueType,
        label: entry.label,
        description: entry.description,
        unit: entry.unit,
        exposed: entry.exposed,
        createdAt: SAVED_AT,
        updatedAt: SAVED_AT,
    };
}

function refusal(status: number, call: string, technicalCode?: string): ObservabilityHttpError {
    return new ObservabilityHttpError(status, `HTTP ${status} on ${call}`, call, undefined, technicalCode);
}

export interface InMemoryMetricDefinitionsSource extends MetricDefinitionsSource {
    /** What the environment holds now, as the next `list()` answers it. */
    readonly entries: () => readonly MetricDefinitionEntry[];
}

let instances = 0;

/**
 * A source answering like `GET/POST/PUT/DELETE metric-definitions` from what the environment holds:
 * a declaration adds a declared row, a save turns a discovered row into a declared one, a deletion
 * sends a key back to discovered while data carries it (`lastSeen`) and drops it otherwise.
 *
 * A key with no record is labelled as the server labels it (the library's `deriveAdditionalMetricLabel`,
 * the same rule as `AdditionalMetricLabel.derive`).
 *
 * Lives here because the library's `testing` entry (6.9.0) declares such a source in its types and
 * ships none in its bundle.
 */

export function createInMemoryMetricDefinitionsSource(initial: readonly MetricDefinitionEntry[]): InMemoryMetricDefinitionsSource {
    let entries: readonly MetricDefinitionEntry[] = [...initial];
    instances += 1;

    return {
        // One scope per instance, as the HTTP source has one base URL per environment: two sources never share a cache.
        scope: `in-memory-${instances}`,
        entries: () => entries,
        async list(): Promise<MetricDefinitionsResponse> {
            return { data: entries, refreshedAt: SAVED_AT, truncated: false };
        },
        async declare(request: DeclareMetricDefinitionRequest): Promise<MetricDefinitionRecord> {
            if (entries.some(entry => entry.name === request.name && entry.origin !== 'DISCOVERED')) {
                throw refusal(400, 'POST metric-definitions', 'observability.metric_definition.duplicate');
            }
            const rawKey = rawKeyOf(request.name);
            const entry: MetricDefinitionEntry = {
                name: request.name,
                rawKey,
                ...typeOf(rawKey),
                label: request.label || deriveAdditionalMetricLabel(rawKey),
                description: request.description || undefined,
                unit: request.unit || undefined,
                apiTypes: [],
                origin: 'DECLARED',
                lastSeen: entries.find(candidate => candidate.name === request.name)?.lastSeen,
                exposed: request.exposed ?? true,
            };
            entries = [...entries.filter(candidate => candidate.name !== request.name), entry];
            return recordOf(entry);
        },
        async save(name: string, request: SaveMetricDefinitionRequest): Promise<SavedMetricDefinition> {
            const existing = entries.find(entry => entry.name === name);
            if (existing?.origin === 'BUILT_IN') {
                throw refusal(400, 'PUT metric-definitions/record', 'observability.metric_definition.built_in');
            }
            // Like the server's PUT: the record on a listed key is replaced, one on an unlisted key is created.
            const rawKey = existing?.rawKey ?? rawKeyOf(name);
            const entry: MetricDefinitionEntry = {
                ...(existing ?? { name, rawKey, ...typeOf(rawKey), apiTypes: [] }),
                origin: 'DECLARED',
                label: request.label || deriveAdditionalMetricLabel(rawKey),
                description: request.description || undefined,
                unit: request.unit || undefined,
                exposed: request.exposed ?? true,
            };
            if (existing) {
                entries = entries.map(candidate => (candidate.name === name ? entry : candidate));
            } else {
                entries = [...entries, entry];
            }
            return { record: recordOf(entry), created: existing?.origin !== 'DECLARED' };
        },
        async remove(name: string): Promise<void> {
            const existing = entries.find(entry => entry.name === name && entry.origin === 'DECLARED');
            if (!existing) {
                throw refusal(404, 'DELETE metric-definitions/record', 'observability.metric_definition.not_found');
            }
            entries = existing.lastSeen
                ? entries.map(candidate =>
                      candidate.name === name
                          ? {
                                ...candidate,
                                origin: 'DISCOVERED',
                                label: deriveAdditionalMetricLabel(candidate.rawKey),
                                description: undefined,
                                unit: undefined,
                                exposed: true,
                            }
                          : candidate,
                  )
                : entries.filter(candidate => candidate.name !== name);
        },
    };
}
