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

import { HealthCheckGlobalMetrics } from './HealthCheckGlobalMetrics';

jest.mock('@gravitee/graphene-charts', () => ({
    ChartContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
    DoughnutChart: () => <div data-testid="doughnut-chart" />,
}));

describe('HealthCheckGlobalMetrics', () => {
    it('shows only the empty copy when availability and response time have no data', () => {
        render(
            <HealthCheckGlobalMetrics
                availability={{ isLoading: false, isError: false }}
                responseTime={{ isLoading: false, isError: false }}
            />,
        );

        expect(screen.getAllByText('No data to display')).toHaveLength(2);
        expect(screen.queryByTestId('doughnut-chart')).not.toBeInTheDocument();
    });
});
