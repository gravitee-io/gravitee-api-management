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

import { HealthCheckLogDetailSheet } from './HealthCheckLogDetailSheet';
import type { HealthCheckLog } from '../../../../types/healthCheck';

const LOG: HealthCheckLog = {
    id: 'log-1',
    timestamp: '2026-04-13T12:30:00Z',
    endpointName: 'endpoint-1',
    gatewayId: 'gateway-a',
    responseTime: 250,
    success: false,
    steps: [
        {
            name: 'default-step',
            success: false,
            message: 'connection refused',
            request: { uri: 'https://api/health', method: 'GET', headers: { Accept: 'application/json' } },
            response: { status: 503, body: 'Service Unavailable', headers: { 'content-type': 'text/plain' } },
        },
    ],
};

describe('HealthCheckLogDetailSheet', () => {
    it('renders nothing visible when no log is selected', () => {
        render(<HealthCheckLogDetailSheet log={null} onClose={jest.fn()} />);
        expect(screen.queryByText('Health check detail')).not.toBeInTheDocument();
    });

    it('renders the summary and request/response steps for a selected log', () => {
        render(<HealthCheckLogDetailSheet log={LOG} onClose={jest.fn()} />);

        expect(screen.getByText('Health check detail')).toBeInTheDocument();
        expect(screen.getByText('endpoint-1')).toBeInTheDocument();
        expect(screen.getByText('gateway-a')).toBeInTheDocument();
        expect(screen.getByText('250 ms')).toBeInTheDocument();
        expect(screen.getByText('connection refused')).toBeInTheDocument();
        expect(screen.getByText(/GET https:\/\/api\/health/)).toBeInTheDocument();
        expect(screen.getByText(/Status: 503/)).toBeInTheDocument();
        expect(screen.getByText('Service Unavailable')).toBeInTheDocument();
    });
});
