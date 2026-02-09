/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { ChartDataset } from 'chart.js';

import { assignChartColors, CHART_COLORS, CHART_BORDER_COLORS } from './chart-colors';

describe('ChartColors', () => {
  it('should assign colors to datasets sequentially', () => {
    const datasets: ChartDataset[] = [{ data: [] }, { data: [] }, { data: [] }];

    assignChartColors(datasets);

    expect(datasets[0].backgroundColor).toBe(CHART_COLORS[0]);
    expect(datasets[1].backgroundColor).toBe(CHART_COLORS[1]);
    expect(datasets[2].backgroundColor).toBe(CHART_COLORS[2]);

    expect(datasets[0].borderColor).toBe(CHART_BORDER_COLORS[0]);
    expect(datasets[1].borderColor).toBe(CHART_BORDER_COLORS[1]);
    expect(datasets[2].borderColor).toBe(CHART_BORDER_COLORS[2]);
  });

  it('should cycle colors if datasets exceed palette length', () => {
    const paletteLength = CHART_COLORS.length;
    const datasets: ChartDataset[] = Array.from({ length: paletteLength + 2 }, () => ({ data: [] }));

    assignChartColors(datasets);
    expect(datasets[0].backgroundColor).toBe(CHART_COLORS[0]);
    expect(datasets[paletteLength].backgroundColor).toBe(CHART_COLORS[0]);
    expect(datasets[paletteLength + 1].backgroundColor).toBe(CHART_COLORS[1]);
  });
});
