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
import { HTTP_PROXY_DASHBOARD } from '../http-proxy.dashboard';
import { observabilityTemplates } from '../index';
import { cartesianSeriesSupportRequestTotal, doughnutStatesItsScopeInTitle } from '../shared';

const widgets = HTTP_PROXY_DASHBOARD.widgets;

const templateWidgets = observabilityTemplates.flatMap(template => template.dashboard.widgets);

// The templates are written on the legacy 12-column grid; the library converts them on read.
const GRID_COLUMNS = 12;

type Cell = { x: number; y: number; cols: number; rows: number };

function overlap(a: Cell, b: Cell): boolean {
    return a.x < b.x + b.cols && b.x < a.x + a.cols && a.y < b.y + b.rows && b.y < a.y + a.rows;
}

describe('HTTP Proxy dashboard template coherence', () => {
    it('cartesian widgets never showTotal on mixed-unit or dual-axis series', () => {
        for (const widget of widgets) {
            if (widget.type !== 'cartesian') continue;

            if (widget.showTotal) {
                expect(cartesianSeriesSupportRequestTotal(widget.series)).toBe(true);
            }
        }
    });

    it('mixed bar+line cartesian widgets declare dual axis and units', () => {
        for (const widget of widgets) {
            if (widget.type !== 'cartesian') continue;

            const hasBar = widget.series.some(s => s.representation === 'bar');
            const hasLine = widget.series.some(s => s.representation === 'line');
            if (!hasBar || !hasLine) continue;

            for (const series of widget.series) {
                expect(series.axisId).toBeDefined();
                expect(series.unit).toBeDefined();
            }
        }
    });

    it('doughnut widgets fill their hole with the total of their slices', () => {
        for (const widget of widgets) {
            if (widget.type !== 'doughnut') continue;

            expect(widget.centerTotal).toBe(true);
        }
    });

    it('capped doughnut widgets state their scope in the title', () => {
        for (const widget of widgets) {
            if (widget.type !== 'doughnut') continue;

            expect(doughnutStatesItsScopeInTitle(widget)).toBe(true);
        }
    });

    it('scopes every widget to HTTP_PROXY', () => {
        for (const widget of widgets) {
            const scopeFilter = widget.filters?.find(f => f.field === 'API_TYPE');
            expect(scopeFilter).toEqual(expect.objectContaining({ operator: 'in', value: ['HTTP_PROXY'] }));
        }
    });

    it('sorts by COUNT only the facet widgets that rank by request count, in every template', () => {
        for (const widget of templateWidgets) {
            if (!('sorts' in widget) || !widget.sorts?.some(sort => sort.measure === 'COUNT')) continue;

            expect('metric' in widget ? widget.metric : undefined).toBeUndefined();
            expect('valueLabel' in widget ? widget.valueLabel : undefined).toBe('Requests');
        }
    });

    it('ranks every request by raw path, and the matched ones by path mapping, in two widgets', () => {
        const topPaths = widgets.find(widget => widget.title === 'Top 5 Paths');
        const topPathMappings = widgets.find(widget => widget.title === 'Top 5 Path mappings');

        expect(topPaths).toEqual(
            expect.objectContaining({
                id: 'http-proxy-top-request-paths',
                type: 'bar',
                by: ['HTTP_PATH'],
                description: 'Most-requested paths, after the API context path, query string excluded.',
            }),
        );
        expect(topPathMappings).toEqual(
            expect.objectContaining({
                id: 'http-proxy-top-paths',
                type: 'bar',
                by: ['HTTP_PATH_MAPPING'],
                description:
                    'Counts only requests that matched a path mapping and passed security (401/403 excluded). Empty for APIs without path mappings.',
            }),
        );
    });

    it('puts the two path widgets side by side, raw paths on the left, in the same shape', () => {
        const topPaths = widgets.find(widget => widget.id === 'http-proxy-top-request-paths')!.layout;
        const topPathMappings = widgets.find(widget => widget.id === 'http-proxy-top-paths')!.layout;

        expect(topPathMappings).toEqual({ ...topPaths, x: topPaths.x + topPaths.cols });
    });

    it('fills every row of the HTTP Proxy overview', () => {
        const rows = new Map<number, number>();
        for (const { layout } of widgets) {
            for (let y = layout.y; y < layout.y + layout.rows; y++) {
                rows.set(y, (rows.get(y) ?? 0) + layout.cols);
            }
        }

        expect(rows.size).toBeGreaterThan(0);
        expect([...rows.entries()].filter(([, cols]) => cols !== GRID_COLUMNS)).toEqual([]);
    });

    it('lays out every template widget inside the grid without overlap', () => {
        for (const template of observabilityTemplates) {
            const cells = template.dashboard.widgets.map(widget => ({ id: widget.id, ...widget.layout }));
            expect(cells.length).toBeGreaterThan(0);

            for (const cell of cells) {
                expect(cell.x + cell.cols).toBeLessThanOrEqual(GRID_COLUMNS);
            }
            const overlaps = cells.flatMap((cell, index) =>
                cells
                    .slice(index + 1)
                    .filter(other => overlap(cell, other))
                    .map(other => `${cell.id} / ${other.id}`),
            );
            expect(overlaps).toEqual([]);
        }
    });

    it('does not reference MCP or LLM-specific dimensions', () => {
        const serialized = JSON.stringify(widgets);
        expect(serialized).not.toMatch(/MCP_PROXY|LLM_PROXY|MCP_PROXY_|LLM_PROXY_/);
    });
});
