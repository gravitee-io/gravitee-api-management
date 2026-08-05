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

import { TooltipProvider } from '@gravitee/graphene-core';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { GatewayInstancesTable } from './GatewayInstancesTable';
import type { GatewayInstanceRow } from '../types/instance';

const ROWS: GatewayInstanceRow[] = [
    {
        id: 'event-1',
        hostname: 'apim-gateway',
        version: '4.12.13',
        state: 'STARTED',
        lastHeartbeat: new Date('2026-08-05T13:45:14Z'),
        os: 'Linux',
        ip: '172.18.0.3',
        port: '8082',
        tenant: '',
        tags: [],
    },
];

describe('GatewayInstancesTable', () => {
    it('renders the hostname as a link to the environment tab (middle-click / new tab)', () => {
        render(
            <MemoryRouter>
                <TooltipProvider>
                    <GatewayInstancesTable
                        rows={ROWS}
                        isLoading={false}
                        page={1}
                        pageSize={10}
                        totalCount={1}
                        onPageChange={jest.fn()}
                        onPageSizeChange={jest.fn()}
                    />
                </TooltipProvider>
            </MemoryRouter>,
        );

        const link = screen.getByTestId('gateway-instance-name-link');
        expect(link.getAttribute('href')).toBe('/event-1/environment');
        expect(link.textContent).toBe('apim-gateway');
    });
});
