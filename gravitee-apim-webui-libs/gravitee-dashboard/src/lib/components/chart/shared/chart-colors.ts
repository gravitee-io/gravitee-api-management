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

/**
 * Default color palette ordered: blue > green > yellow > orange > red
 * This ensures HTTP status codes display intuitively (2xx in blue/green, 5xx in red)
 */
export const CHART_COLORS = [
  'rgba(54, 162, 235, 0.8)', // Blue
  'rgba(75, 192, 192, 0.8)', // Teal/Green
  'rgba(255, 206, 86, 0.8)', // Yellow
  'rgba(255, 159, 64, 0.8)', // Orange
  'rgba(255, 99, 132, 0.8)', // Red
  'rgba(153, 102, 255, 0.8)', // Purple
  'rgba(201, 203, 207, 0.8)', // Grey
  'rgba(255, 105, 180, 0.8)', // Pink
  'rgba(139, 69, 19, 0.8)', // Brown
  'rgba(127, 255, 0, 0.8)', // Green Lemon
];

export const CHART_BORDER_COLORS = [
  'rgba(54, 162, 235, 1)',
  'rgba(75, 192, 192, 1)',
  'rgba(255, 206, 86, 1)',
  'rgba(255, 159, 64, 1)',
  'rgba(255, 99, 132, 1)',
  'rgba(153, 102, 255, 1)',
  'rgba(201, 203, 207, 1)',
  'rgba(255, 105, 180, 1)', // Pink
  'rgba(139, 69, 19, 1)', // Brown
  'rgba(127, 255, 0, 1)', // Green Lemon
];

export const assignChartColors = (datasets: ChartDataset[]) => {
  datasets.forEach((dataset, index) => {
    dataset.backgroundColor = CHART_COLORS[index % CHART_COLORS.length];
    dataset.borderColor = CHART_BORDER_COLORS[index % CHART_BORDER_COLORS.length];
  });
};
