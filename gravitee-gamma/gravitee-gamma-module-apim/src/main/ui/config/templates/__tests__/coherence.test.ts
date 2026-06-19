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
import { cartesianSeriesSupportRequestTotal } from '../shared';

const widgets = HTTP_PROXY_DASHBOARD.widgets;

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

    it('scopes every widget to HTTP_PROXY', () => {
        for (const widget of widgets) {
            const scopeFilter = widget.filters?.find(f => f.field === 'API_TYPE');
            expect(scopeFilter).toEqual(expect.objectContaining({ operator: 'in', value: ['HTTP_PROXY'] }));
        }
    });

    it('does not reference MCP or LLM-specific dimensions', () => {
        const serialized = JSON.stringify(widgets);
        expect(serialized).not.toMatch(/MCP_PROXY|LLM_PROXY|MCP_PROXY_|LLM_PROXY_/);
    });
});
