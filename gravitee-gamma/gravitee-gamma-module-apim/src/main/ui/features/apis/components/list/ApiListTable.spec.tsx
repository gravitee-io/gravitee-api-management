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
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, useNavigate } from 'react-router-dom';

import { ApiListTable } from './ApiListTable';
import type { ApiListItem, ApiListOriginContext } from '../../types';

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: jest.fn(),
}));

jest.mock('@gravitee/gamma-lib-observability', () => ({
    DEFAULT_TIME_RANGE: { type: 'relative', period: '5m' },
    encodeObservabilityState: () => ({ q: 'ENCODED_Q', v: '1' }),
}));

const mockNavigate = jest.fn();

function makeApi(overrides: Partial<ApiListItem> = {}): ApiListItem {
    return { id: 'api-1', name: 'Test API', apiVersion: '1.0', type: 'PROXY', definitionVersion: 'V4', ...overrides };
}

function renderTable(props: Partial<Parameters<typeof ApiListTable>[0]> = {}) {
    return render(
        <MemoryRouter>
            <ApiListTable apis={[]} isLoading={false} {...props} />
        </MemoryRouter>,
    );
}

function originCellOf(row: HTMLElement) {
    const originColumnIndex = screen.getAllByRole('columnheader').findIndex(header => header.textContent === 'Origin');
    return within(row).getAllByRole('cell')[originColumnIndex];
}

describe('ApiListTable', () => {
    beforeEach(() => {
        (useNavigate as jest.Mock).mockReturnValue(mockNavigate);
    });

    afterEach(() => jest.clearAllMocks());

    it('renders skeleton rows when loading', async () => {
        const { container } = renderTable({ isLoading: true, skeletonRowCount: 3 });
        // Each skeleton row has 5 Skeleton elements. DataTable defers skeletons by ~200ms (loadingDelay)
        // to prevent flash on fast requests, so we have to wait for them.
        await waitFor(() => {
            const skeletons = container.querySelectorAll('[class*="animate-pulse"]');
            expect(skeletons.length).toBeGreaterThan(0);
        });
    });

    it('renders the empty state when no APIs are present', () => {
        renderTable({ apis: [], isLoading: false });
        expect(screen.queryByText(/no apis found/i)).not.toBeNull();
    });

    it('renders a row for each API with the name', () => {
        const api = makeApi({ name: 'My Service' });
        renderTable({ apis: [api] });
        expect(screen.queryByText('My Service')).not.toBeNull();
    });

    it('navigates to the overview page on row click', () => {
        const api = makeApi();
        renderTable({ apis: [api] });
        fireEvent.click(screen.getByText('Test API'));
        expect(mockNavigate).toHaveBeenCalledWith('api-1/overview');
    });

    it('renders the API actions button for each row', () => {
        const api = makeApi();
        renderTable({ apis: [api] });
        expect(screen.queryByRole('button', { name: 'API actions' })).not.toBeNull();
    });

    it('stops row click propagation when the actions button is clicked', () => {
        const api = makeApi();
        renderTable({ apis: [api] });
        fireEvent.click(screen.getByRole('button', { name: 'API actions' }));
        expect(mockNavigate).not.toHaveBeenCalledWith('api-1/overview');
    });

    it('opens the actions dropdown and shows navigation items', async () => {
        const user = userEvent.setup();
        const api = makeApi();
        renderTable({ apis: [api] });
        await user.click(screen.getByRole('button', { name: 'API actions' }));
        await waitFor(() => expect(screen.queryByText('View Details')).not.toBeNull());
        expect(screen.queryByText('Edit Configuration')).not.toBeNull();
        expect(screen.queryByText('View Analytics')).not.toBeNull();
    });

    it('navigates "View Analytics" to the API-filtered observability dashboard', async () => {
        const user = userEvent.setup();
        renderTable({ apis: [makeApi()] });
        await user.click(screen.getByRole('button', { name: 'API actions' }));
        await user.click(await screen.findByText('View Analytics'));
        expect(mockNavigate).toHaveBeenCalledWith(expect.stringContaining('../observe/dashboards/http-proxy-overview?'));
    });

    describe('RuntimeStatusBadge', () => {
        it('shows "Started" badge for STARTED state', () => {
            renderTable({ apis: [makeApi({ state: 'STARTED' })] });
            expect(screen.queryByText('Started')).not.toBeNull();
        });

        it('shows "Stopped" badge for STOPPED state', () => {
            renderTable({ apis: [makeApi({ state: 'STOPPED' })] });
            expect(screen.queryByText('Stopped')).not.toBeNull();
        });

        it('shows "Closed" badge for CLOSED state', () => {
            renderTable({ apis: [makeApi({ state: 'CLOSED' })] });
            expect(screen.queryByText('Closed')).not.toBeNull();
        });
    });

    describe('SyncStatusBadge', () => {
        it('shows "In sync" badge for DEPLOYED state', () => {
            renderTable({ apis: [makeApi({ deploymentState: 'DEPLOYED' })] });
            expect(screen.queryByText('In sync')).not.toBeNull();
        });

        it('shows "Out of sync" badge for NEED_REDEPLOY state', () => {
            renderTable({ apis: [makeApi({ deploymentState: 'NEED_REDEPLOY' })] });
            expect(screen.queryByText('Out of sync')).not.toBeNull();
        });
    });

    describe('OriginIndicator', () => {
        it.each<[number, string, Partial<ApiListItem>]>([
            [1, 'an integration-sourced row', { originContext: { origin: 'INTEGRATION', provider: 'solace' } }],
            [0, 'a natively-managed row', { originContext: { origin: 'MANAGEMENT' } }],
            [0, 'a Kubernetes-managed row', { originContext: { origin: 'KUBERNETES' } }],
            [0, 'a row carrying no origin context', {}],
            // BaseOriginContext declares no required members, so the discriminator can be absent on the wire;
            // the indicator is gated on `origin`, never inferred from a `provider` arriving without it.
            [0, 'a row carrying a provider but no origin discriminator', { originContext: { provider: 'solace' } }],
        ])('renders %i origin indicator element for %s', (expectedCount, _scenario, overrides) => {
            renderTable({ apis: [makeApi(overrides)] });
            expect(screen.queryAllByTestId('api-origin-indicator')).toHaveLength(expectedCount);
        });

        it('renders the Origin header as plain text, unlike the server-sortable API Name header', () => {
            renderTable({ apis: [makeApi({ originContext: { origin: 'INTEGRATION', provider: 'solace' } })] });
            // `toApiListSortBy` returns undefined for Origin, so a sort control here would be dead.
            expect(within(screen.getByRole('columnheader', { name: 'Origin' })).queryByRole('button')).toBeNull();
            expect(within(screen.getByRole('columnheader', { name: 'API Name' })).queryByRole('button')).not.toBeNull();
        });

        it('adds the Origin column right after API Name without displacing any other column', () => {
            renderTable({ apis: [makeApi({ originContext: { origin: 'INTEGRATION', provider: 'solace' } })] });
            expect(screen.getAllByRole('columnheader').map(header => header.textContent)).toEqual([
                'API Name',
                'Origin',
                'Runtime Status',
                'Sync Status',
                'Access',
                'Sharding Tags',
                'Owner',
                'Actions',
            ]);
        });

        // The cell's exact text is asserted alongside the indicator count so that nothing — a dash, a
        // "Managed" caption, a differently-named element — can be substituted where the indicator is absent.
        it.each<[string, Partial<ApiListItem>, number, string]>([
            ['the provider name', { originContext: { origin: 'INTEGRATION', provider: 'solace' } }, 1, 'Solace'],
            ['nothing at all', { originContext: { origin: 'MANAGEMENT' } }, 0, ''],
        ])('holds %s in the cell under the Origin header', (_scenario, overrides, expectedIndicatorCount, expectedCellText) => {
            renderTable({ apis: [makeApi(overrides)] });
            const [, dataRow] = screen.getAllByRole('row');
            const originCell = originCellOf(dataRow);
            expect(within(originCell).queryAllByTestId('api-origin-indicator')).toHaveLength(expectedIndicatorCount);
            expect(originCell.textContent).toBe(expectedCellText);
        });

        it.each<[string, string]>([
            ['aws-api-gateway', 'AWS API Gateway'],
            ['solace', 'Solace'],
            ['apigee', 'Apigee'],
            ['azure-api-management', 'Azure API Management'],
            ['ibm-api-connect', 'IBM API Connect'],
            ['confluent-platform', 'Confluent Platform'],
            ['mulesoft', 'MuleSoft'],
            ['edge-stack', 'Edge Stack'],
        ])('shows the display name of the %s provider rather than its raw code', (provider, displayName) => {
            renderTable({ apis: [makeApi({ originContext: { origin: 'INTEGRATION', provider } })] });
            expect(screen.getByTestId('api-origin-indicator').textContent).toBe(displayName);
            expect(screen.queryByText(provider)).toBeNull();
        });

        it('shows the same display name for the uppercase AWS alias as for aws-api-gateway', () => {
            renderTable({
                apis: [
                    makeApi({ id: 'aws-alias', name: 'Legacy AWS API', originContext: { origin: 'INTEGRATION', provider: 'AWS' } }),
                    makeApi({
                        id: 'aws-canonical',
                        name: 'Canonical AWS API',
                        originContext: { origin: 'INTEGRATION', provider: 'aws-api-gateway' },
                    }),
                ],
            });
            const [aliasLabel, canonicalLabel] = screen.getAllByTestId('api-origin-indicator').map(indicator => indicator.textContent);
            expect(aliasLabel).toBe(canonicalLabel);
            expect(aliasLabel).toBe('AWS API Gateway');
        });

        it.each<[string, string]>([
            ['absent from the display-name map', 'mycompany-gateway'],
            // A bare object index would resolve this key on Object.prototype and render a function instead.
            ['colliding with an Object.prototype member', 'toString'],
        ])('shows a provider code %s verbatim', (_scenario, provider) => {
            renderTable({ apis: [makeApi({ originContext: { origin: 'INTEGRATION', provider } })] });
            expect(screen.getByTestId('api-origin-indicator').textContent).toBe(provider);
        });

        it('keeps each row indicator on its own origin context when rows of both origins are listed together', () => {
            renderTable({
                apis: [
                    makeApi({ id: 'federated-solace', name: 'Orders API', originContext: { origin: 'INTEGRATION', provider: 'solace' } }),
                    makeApi({ id: 'native', name: 'Payments API', originContext: { origin: 'MANAGEMENT' } }),
                    makeApi({ id: 'federated-apigee', name: 'Billing API', originContext: { origin: 'INTEGRATION', provider: 'apigee' } }),
                ],
            });
            const [, solaceRow, nativeRow, apigeeRow] = screen.getAllByRole('row');
            expect(within(solaceRow).getByTestId('api-origin-indicator').textContent).toBe('Solace');
            expect(within(nativeRow).queryAllByTestId('api-origin-indicator')).toHaveLength(0);
            expect(within(apigeeRow).getByTestId('api-origin-indicator').textContent).toBe('Apigee');
        });

        // `provider` is @Nullable on the backend record (OriginContext.java:57), so an explicit
        // JSON null reaches the client even though the client type only models it as optional.
        const nullProvider = null as unknown as string;

        it.each<[string, ApiListOriginContext]>([
            ['null', { origin: 'INTEGRATION', provider: nullProvider }],
            ['absent', { origin: 'INTEGRATION' }],
            ['empty', { origin: 'INTEGRATION', provider: '' }],
        ])('shows the em dash when the provider is %s', (_scenario, originContext) => {
            renderTable({ apis: [makeApi({ originContext })] });
            expect(screen.getByTestId('api-origin-indicator').textContent).toBe('—');
        });
    });

    it('renders access path when listener has an HTTP path', () => {
        const api = makeApi({ listeners: [{ type: 'HTTP', paths: [{ path: '/my-api' }] }] });
        renderTable({ apis: [api] });
        expect(screen.queryByText('/my-api')).not.toBeNull();
    });

    it('renders owner display name', () => {
        const api = makeApi({ primaryOwner: { displayName: 'Jane Doe' } });
        renderTable({ apis: [api] });
        expect(screen.queryByText('Jane Doe')).not.toBeNull();
    });
});
