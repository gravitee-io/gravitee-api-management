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
import type { CartesianWidget, FilterCondition, SeriesDefinition, Widget } from '@gravitee/gamma-lib-observability';

export function apiTypeScope(value: string): FilterCondition {
    return { field: 'API_TYPE', label: 'API Type', operator: 'in', value: [value], valueLabels: [value] };
}

/**
 * The chart tooltip total sums every series value. That only makes sense when
 * all series share the same unit and axis (e.g. stacked request bars).
 */
export function cartesianSeriesSupportRequestTotal(series: readonly SeriesDefinition[]): boolean {
    if (series.length === 0) return false;

    const axisIds = new Set(series.map(s => s.axisId ?? 'left'));
    const units = new Set(series.map(s => s.unit));
    const representations = new Set(series.map(s => s.representation));

    if (axisIds.size > 1 || units.size > 1) return false;
    if (representations.has('bar') && representations.has('line')) return false;

    return true;
}

function stripCartesianTooltipTotal(widget: CartesianWidget): CartesianWidget {
    if (!widget.showTotal || cartesianSeriesSupportRequestTotal(widget.series)) return widget;

    const { showTotal: _showTotal, totalLabel: _totalLabel, ...rest } = widget;
    return rest;
}

function sanitizeWidget(widget: Widget): Widget {
    return widget.type === 'cartesian' ? stripCartesianTooltipTotal(widget) : widget;
}

/**
 * Pins every widget of a dashboard to a single `API_TYPE`. Interim workaround
 * until the lib honours per-dashboard scope (GMA-414).
 */
export function scopeWidgets(apiType: string, widgets: Widget[]): Widget[] {
    const scope = apiTypeScope(apiType);
    return widgets.map(widget => sanitizeWidget({ ...widget, filters: [...(widget.filters ?? []), scope] }));
}

/** Deterministic top-N ordering for facet widgets (bar/doughnut). */
export const TOP_BY_COUNT = [{ measure: 'COUNT', order: 'DESC' }] as const;
