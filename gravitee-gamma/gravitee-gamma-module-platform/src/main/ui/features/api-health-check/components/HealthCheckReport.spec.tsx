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

import { HealthCheckReport } from './HealthCheckReport';

describe('HealthCheckReport', () => {
    it('reports all operational when nothing is degraded, like Classic', () => {
        render(<HealthCheckReport report={{ inError: 0, inWarning: 0 }} isLoading={false} isError={false} />);

        expect(screen.getByText('API Health Check Report')).not.toBeNull();
        expect(screen.getByText('All APIs are operational')).not.toBeNull();
    });

    it('never shows an operational count, because the table is paginated and no backend aggregates it', () => {
        render(<HealthCheckReport report={{ inError: 1, inWarning: 2 }} isLoading={false} isError={false} />);

        expect(screen.queryByText(/operational/i)).toBeNull();
    });

    it('names the APIs in error and in warning with Classic thresholds', () => {
        render(<HealthCheckReport report={{ inError: 2, inWarning: 3 }} isLoading={false} isError={false} />);

        expect(screen.getByText('2 APIs are in error (HealthCheck availability <= 80%)')).not.toBeNull();
        expect(screen.getByText('3 APIs are in warning (HealthCheck availability <= 95%)')).not.toBeNull();
    });

    it('uses the singular form for a single API, like Classic', () => {
        render(<HealthCheckReport report={{ inError: 1, inWarning: 1 }} isLoading={false} isError={false} />);

        expect(screen.getByText('1 API is in error (HealthCheck availability <= 80%)')).not.toBeNull();
        expect(screen.getByText('1 API is in warning (HealthCheck availability <= 95%)')).not.toBeNull();
    });

    it('omits the warning line when only errors are present', () => {
        render(<HealthCheckReport report={{ inError: 1, inWarning: 0 }} isLoading={false} isError={false} />);

        expect(screen.getByText(/in error/)).not.toBeNull();
        expect(screen.queryByText(/in warning/)).toBeNull();
    });

    it('shows Loading while the report is still being gathered', () => {
        render(<HealthCheckReport isLoading isError={false} />);

        expect(screen.getByText('Loading...')).not.toBeNull();
        expect(screen.queryByText('All APIs are operational')).toBeNull();
    });

    it('surfaces a failed report instead of claiming everything is operational', () => {
        render(<HealthCheckReport isLoading={false} isError />);

        expect(screen.getByText('Failed to load the API Health Check report.')).not.toBeNull();
        expect(screen.queryByText('All APIs are operational')).toBeNull();
    });
});
