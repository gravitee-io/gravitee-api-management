/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { render, screen } from '@testing-library/react';

import { CLOUD_GATEWAYS_MOCK } from '../cloud.config';
import { CloudGatewaysCard } from './CloudGatewaysCard';

describe('CloudGatewaysCard', () => {
    it('renders Cockpit-style gateways table with deploy action', () => {
        render(<CloudGatewaysCard gateways={CLOUD_GATEWAYS_MOCK} hasEnvironments onDeployGateway={() => undefined} />);

        expect(screen.getByText('Gateways')).toBeTruthy();
        expect(screen.getByText('Manage how your Gateways will be deployed.')).toBeTruthy();
        expect(screen.getByTestId('deploy-gateways-button')).toBeTruthy();
        expect(screen.getAllByTestId('gateway-list-row').length).toBe(2);
        expect(screen.getByText('Not Connected')).toBeTruthy();
        expect(screen.queryByPlaceholderText('Filter')).toBeNull();
    });

    it('renders empty state when there are no gateways', () => {
        render(<CloudGatewaysCard gateways={[]} hasEnvironments onDeployGateway={() => undefined} />);

        expect(screen.getByText('No Gateways... yet')).toBeTruthy();
        expect(screen.getByText('Manage how your Gateways will be configured.')).toBeTruthy();
    });
});
