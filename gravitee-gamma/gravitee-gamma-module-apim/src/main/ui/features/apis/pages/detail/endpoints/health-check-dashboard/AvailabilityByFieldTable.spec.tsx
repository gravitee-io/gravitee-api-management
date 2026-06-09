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

import { AvailabilityByFieldTable } from './AvailabilityByFieldTable';
import type { AvailabilityFieldData } from './useHealthCheckDashboard';

function buildRows(count: number) {
    return Array.from({ length: count }, (_, i) => ({
        key: `ep-${i}`,
        name: `endpoint-${i}`,
        availabilityPct: 99,
        avgResponseTimeMs: 10 + i,
    }));
}

function buildData(rowCount: number): AvailabilityFieldData {
    return { rows: buildRows(rowCount), isLoading: false, isError: false };
}

describe('AvailabilityByFieldTable', () => {
    it('paginates client-side with a default page size of 5', () => {
        render(<AvailabilityByFieldTable field="endpoint" data={buildData(8)} />);

        // First page shows 5 rows…
        expect(screen.getByText('endpoint-0')).toBeInTheDocument();
        expect(screen.getByText('endpoint-4')).toBeInTheDocument();
        // …and not the 6th.
        expect(screen.queryByText('endpoint-5')).not.toBeInTheDocument();
    });

    it('advances to the next page of rows', async () => {
        render(<AvailabilityByFieldTable field="gateway" data={buildData(8)} />);

        await userEvent.click(screen.getByRole('button', { name: /next page/i }));

        expect(screen.getByText('endpoint-5')).toBeInTheDocument();
        expect(screen.queryByText('endpoint-0')).not.toBeInTheDocument();
    });

    it('shows an empty state and no pagination when there are no rows', () => {
        render(<AvailabilityByFieldTable field="endpoint" data={buildData(0)} />);

        expect(screen.getByText('No data')).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /next page/i })).not.toBeInTheDocument();
    });
});
