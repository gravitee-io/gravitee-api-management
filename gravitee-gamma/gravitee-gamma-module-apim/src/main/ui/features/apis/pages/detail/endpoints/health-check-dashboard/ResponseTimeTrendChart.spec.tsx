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
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { ResponseTimeTrendChart } from './ResponseTimeTrendChart';

jest.mock('@gravitee/graphene-charts', () => ({
    ChartContainer: ({ children }: { children: React.ReactNode }) => <div data-testid="chart-container">{children}</div>,
    LineChart: () => <div data-testid="line-chart" />,
    AreaChart: () => <div data-testid="area-chart" />,
    BarChart: () => <div data-testid="bar-chart" />,
}));

const POINTS = [
    { timestamp: 1_700_000_000_000, responseTime: 42 },
    { timestamp: 1_700_000_060_000, responseTime: 87 },
];

describe('ResponseTimeTrendChart', () => {
    it('renders a line chart by default', () => {
        render(<ResponseTimeTrendChart points={POINTS} isLoading={false} />);
        expect(screen.getByTestId('line-chart')).toBeInTheDocument();
        expect(screen.queryByTestId('bar-chart')).not.toBeInTheDocument();
    });

    it('switches chart type when a different type is selected', async () => {
        render(<ResponseTimeTrendChart points={POINTS} isLoading={false} />);

        await userEvent.click(screen.getByRole('button', { name: 'Bar' }));
        expect(screen.getByTestId('bar-chart')).toBeInTheDocument();
        expect(screen.queryByTestId('line-chart')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', { name: 'Area' }));
        expect(screen.getByTestId('area-chart')).toBeInTheDocument();
    });
});
