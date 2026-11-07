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
import { type MetricDefinitionEntry, ObservabilityHttpError } from '@gravitee/gamma-lib-observability';

import { createInMemoryMetricDefinitionsSource } from './inMemoryMetricDefinitionsSource';

const BUILT_IN: MetricDefinitionEntry = {
    name: 'mcp-proxy.tool',
    label: 'Tool',
    apiTypes: ['MCP'],
    origin: 'BUILT_IN',
    rawKey: 'keyword_mcp-proxy_tool',
    valueType: 'KEYWORD',
    exposed: true,
};

const DISCOVERED: MetricDefinitionEntry = {
    name: 'additional-metrics.keyword_checkout_channel',
    label: 'Checkout channel',
    apiTypes: [],
    origin: 'DISCOVERED',
    rawKey: 'keyword_checkout_channel',
    valueType: 'KEYWORD',
    lastSeen: '2026-11-04T08:00:00Z',
    exposed: true,
};

async function entry(source: ReturnType<typeof createInMemoryMetricDefinitionsSource>, name: string): Promise<MetricDefinitionEntry> {
    const found = (await source.list()).data.find(candidate => candidate.name === name);
    if (!found) throw new Error(`${name} is not listed`);
    return found;
}

describe('createInMemoryMetricDefinitionsSource', () => {
    it('declares a key as a declared row, labelled as given or as the server derives it', async () => {
        const source = createInMemoryMetricDefinitionsSource([BUILT_IN]);

        await source.declare({ name: 'additional-metrics.double_checkout_basket-value', label: 'Basket value', unit: '€' });
        await source.declare({ name: 'additional-metrics.long_failover_count' });

        expect(await entry(source, 'additional-metrics.double_checkout_basket-value')).toMatchObject({
            origin: 'DECLARED',
            rawKey: 'double_checkout_basket-value',
            valueType: 'DOUBLE',
            type: 'number',
            label: 'Basket value',
            unit: '€',
            exposed: true,
        });
        expect(await entry(source, 'additional-metrics.long_failover_count')).toMatchObject({
            origin: 'DECLARED',
            label: 'Failover count',
        });
    });

    it('refuses a second declaration of a declared key with the server code', async () => {
        const source = createInMemoryMetricDefinitionsSource([]);
        await source.declare({ name: 'additional-metrics.keyword_a' });

        await expect(source.declare({ name: 'additional-metrics.keyword_a' })).rejects.toMatchObject({
            status: 400,
            technicalCode: 'observability.metric_definition.duplicate',
        });
    });

    it('gives every instance its own scope: two sources never share a cache', () => {
        expect(createInMemoryMetricDefinitionsSource([]).scope).not.toBe(createInMemoryMetricDefinitionsSource([]).scope);
    });

    it('creates the record on save of an unlisted key, as the server does', async () => {
        const source = createInMemoryMetricDefinitionsSource([BUILT_IN]);

        const saved = await source.save('additional-metrics.long_failover_count', { label: 'Failovers', exposed: false });

        expect(saved.created).toBe(true);
        expect(await entry(source, 'additional-metrics.long_failover_count')).toMatchObject({
            origin: 'DECLARED',
            rawKey: 'long_failover_count',
            valueType: 'LONG',
            label: 'Failovers',
            exposed: false,
        });
        expect((await source.save('additional-metrics.long_failover_count', { label: 'Failover count' })).created).toBe(false);
    });

    it('turns a discovered key into a declared one on save, and refuses a built-in', async () => {
        const source = createInMemoryMetricDefinitionsSource([BUILT_IN, DISCOVERED]);

        const saved = await source.save(DISCOVERED.name, { label: 'Channel', exposed: false });

        expect(saved.created).toBe(true);
        expect(await entry(source, DISCOVERED.name)).toMatchObject({
            origin: 'DECLARED',
            label: 'Channel',
            exposed: false,
            lastSeen: DISCOVERED.lastSeen,
        });
        await expect(source.save(BUILT_IN.name, { label: 'Nope' })).rejects.toBeInstanceOf(ObservabilityHttpError);
    });

    it('sends a deleted key back to discovered while data carries it, and drops it otherwise', async () => {
        const source = createInMemoryMetricDefinitionsSource([DISCOVERED]);
        await source.save(DISCOVERED.name, { label: 'Channel', description: 'Where the basket was paid', unit: 'none' });
        await source.declare({ name: 'additional-metrics.keyword_never-seen', label: 'Never seen' });

        await source.remove(DISCOVERED.name);
        await source.remove('additional-metrics.keyword_never-seen');

        expect(await entry(source, DISCOVERED.name)).toMatchObject({ origin: 'DISCOVERED', label: 'Checkout channel', exposed: true });
        expect((await entry(source, DISCOVERED.name)).description).toBeUndefined();
        expect((await source.list()).data.map(candidate => candidate.name)).not.toContain('additional-metrics.keyword_never-seen');
        await expect(source.remove('additional-metrics.keyword_never-seen')).rejects.toMatchObject({ status: 404 });
    });
});
