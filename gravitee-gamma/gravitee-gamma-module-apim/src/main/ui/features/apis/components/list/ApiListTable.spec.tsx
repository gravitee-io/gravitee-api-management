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
import type { ApiDeploymentState, ApiListItem, ApiListOriginContext, ApiState } from '../../types';

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

const federatedOrigin: ApiListOriginContext = { origin: 'INTEGRATION', provider: 'solace' };

// A federated origin context alone leaves `definitionVersion: 'V4'`, which `isFederatedApiListItem`
// reads as natively-managed — so a row built that way is not the FEDERATED subject these cases mean.
function makeFederatedApi(overrides: Partial<ApiListItem> = {}): ApiListItem {
    return makeApi({ definitionVersion: 'FEDERATED', originContext: federatedOrigin, ...overrides });
}

function cellUnderHeader(row: HTMLElement, headerText: string) {
    // Resolved through getByRole rather than an index scan so a renamed header fails with Testing
    // Library naming the header it could not find, instead of an undefined-cell TypeError.
    const header = screen.getByRole('columnheader', { name: headerText });
    const columnIndex = screen.getAllByRole('columnheader').indexOf(header);
    return within(row).getAllByRole('cell')[columnIndex];
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

    it('renders a blank empty-state cell instead of the no-match message when the list failed to load', () => {
        renderTable({ apis: [], isLoading: false, loadFailed: true });

        const [, emptyRow] = screen.getAllByRole('row');
        expect(within(emptyRow).getByRole('cell').textContent).toBe('');
        expect(screen.queryByText('No APIs found')).toBeNull();
    });

    it('renders a row for each API with the name', () => {
        const api = makeApi({ name: 'My Service' });
        renderTable({ apis: [api] });
        expect(screen.queryByText('My Service')).not.toBeNull();
    });

    it('navigates to the overview page on a natively-managed row click', () => {
        const api = makeApi();
        renderTable({ apis: [api] });
        fireEvent.click(screen.getByText('Test API'));
        expect(mockNavigate).toHaveBeenCalledWith('api-1/overview');
    });

    it('navigates to the general page on a federated row click', () => {
        renderTable({ apis: [makeApi({ id: 'federated-1', name: 'Orders API', definitionVersion: 'FEDERATED' })] });
        fireEvent.click(screen.getByText('Orders API'));
        expect(mockNavigate).toHaveBeenCalledWith('federated-1/general');
        expect(mockNavigate).not.toHaveBeenCalledWith('federated-1/overview');
    });

    // Every kind arrives in the same response, so the landing route has to be decided per row: a predicate
    // hoisted out of the cell would still satisfy every single-row case above while sending a whole
    // mixed list to one route. The agent row carries the same INTEGRATION origin as the federated one, so
    // only a predicate keyed on `definitionVersion` can still send it to Overview.
    it('sends each row to the landing route of its own kind when several kinds are listed together', () => {
        renderTable({
            apis: [
                makeApi({ id: 'native', name: 'Payments API' }),
                makeApi({ id: 'federated-1', name: 'Orders API', definitionVersion: 'FEDERATED', originContext: federatedOrigin }),
                makeApi({ id: 'agent-1', name: 'Support Agent', definitionVersion: 'FEDERATED_AGENT', originContext: federatedOrigin }),
            ],
        });

        fireEvent.click(screen.getByText('Payments API'));
        fireEvent.click(screen.getByText('Orders API'));
        fireEvent.click(screen.getByText('Support Agent'));

        expect(mockNavigate.mock.calls).toEqual([['native/overview'], ['federated-1/general'], ['agent-1/overview']]);
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

    it('offers a natively-managed row all three shipped actions and nothing else', async () => {
        const user = userEvent.setup();
        renderTable({ apis: [makeApi()] });
        await user.click(screen.getByRole('button', { name: 'API actions' }));
        await waitFor(() => expect(screen.getAllByRole('menuitem')).toHaveLength(3));
        expect(screen.getAllByRole('menuitem').map(item => item.textContent)).toEqual([
            'View Details',
            'Edit Configuration',
            'View Analytics',
        ]);
    });

    it('offers a federated row only the two actions a federated API has backing data for', async () => {
        const user = userEvent.setup();
        renderTable({ apis: [makeApi({ id: 'federated-1', name: 'Orders API', definitionVersion: 'FEDERATED' })] });
        await user.click(screen.getByRole('button', { name: 'API actions' }));
        // The item count is asserted rather than just the two expected labels, since a menu that still
        // rendered "View Analytics" would satisfy a presence-only check on the other two.
        await waitFor(() => expect(screen.getAllByRole('menuitem')).toHaveLength(2));
        expect(screen.getAllByRole('menuitem').map(item => item.textContent)).toEqual(['View Details', 'Edit Configuration']);
        expect(screen.queryByText('View Analytics')).toBeNull();
    });

    it('navigates "View Details" to the overview page for a natively-managed row', async () => {
        const user = userEvent.setup();
        renderTable({ apis: [makeApi()] });
        await user.click(screen.getByRole('button', { name: 'API actions' }));
        await user.click(await screen.findByText('View Details'));
        expect(mockNavigate).toHaveBeenCalledWith('api-1/overview');
    });

    it('navigates "View Details" to the general page for a federated row', async () => {
        const user = userEvent.setup();
        renderTable({ apis: [makeApi({ id: 'federated-1', name: 'Orders API', definitionVersion: 'FEDERATED' })] });
        await user.click(screen.getByRole('button', { name: 'API actions' }));
        await user.click(await screen.findByText('View Details'));
        expect(mockNavigate).toHaveBeenCalledWith('federated-1/general');
        expect(mockNavigate).not.toHaveBeenCalledWith('federated-1/overview');
    });

    // Each single-row case above holds one kind, so a landing route resolved once for the whole menu column
    // would still satisfy both. Only a mixed list forces "View Details" to resolve from the row it belongs
    // to, and the item-set case that would otherwise catch it asserts a menu composition this file's
    // destination cases must not depend on.
    it('navigates "View Details" to the landing route of each row\'s own kind when both kinds are listed together', async () => {
        const user = userEvent.setup();
        renderTable({
            apis: [
                makeApi({ id: 'native', name: 'Payments API' }),
                makeApi({ id: 'federated-1', name: 'Orders API', definitionVersion: 'FEDERATED' }),
            ],
        });
        const [nativeTrigger, federatedTrigger] = screen.getAllByRole('button', { name: 'API actions' });

        await user.click(nativeTrigger);
        await user.click(await screen.findByText('View Details'));
        // Radix keeps the open menu modal, so the second trigger is unreachable until the first closes.
        await waitFor(() => expect(screen.queryAllByRole('menuitem')).toHaveLength(0));
        await user.click(federatedTrigger);
        await user.click(await screen.findByText('View Details'));

        expect(mockNavigate.mock.calls).toEqual([['native/overview'], ['federated-1/general']]);
    });

    // "Edit Configuration" goes to General for every row, unlike "View Details" — so a handler rewired
    // through the landing helper when the menu took the whole row instead of an id would send a
    // natively-managed row here to `overview`, and every other case in this file would stay green.
    it('navigates "Edit Configuration" to the general page for both row kinds', async () => {
        const user = userEvent.setup();
        renderTable({
            apis: [
                makeApi({ id: 'native', name: 'Payments API' }),
                makeApi({ id: 'federated-1', name: 'Orders API', definitionVersion: 'FEDERATED' }),
            ],
        });
        const [nativeTrigger, federatedTrigger] = screen.getAllByRole('button', { name: 'API actions' });

        await user.click(nativeTrigger);
        await user.click(await screen.findByText('Edit Configuration'));
        // Radix keeps the open menu modal, so the second trigger is unreachable until the first closes.
        await waitFor(() => expect(screen.queryAllByRole('menuitem')).toHaveLength(0));
        await user.click(federatedTrigger);
        await user.click(await screen.findByText('Edit Configuration'));

        expect(mockNavigate.mock.calls).toEqual([['native/general'], ['federated-1/general']]);
    });

    it('offers each row the actions of its own kind when both kinds are listed together', async () => {
        const user = userEvent.setup();
        renderTable({
            apis: [
                makeApi({ id: 'native', name: 'Payments API' }),
                makeApi({ id: 'federated-1', name: 'Orders API', definitionVersion: 'FEDERATED' }),
            ],
        });
        const [nativeTrigger, federatedTrigger] = screen.getAllByRole('button', { name: 'API actions' });

        await user.click(nativeTrigger);
        await waitFor(() =>
            expect(screen.getAllByRole('menuitem').map(item => item.textContent)).toEqual([
                'View Details',
                'Edit Configuration',
                'View Analytics',
            ]),
        );

        // Radix keeps the open menu modal, so the second trigger is unreachable until the first closes.
        await user.keyboard('{Escape}');
        await waitFor(() => expect(screen.queryAllByRole('menuitem')).toHaveLength(0));

        await user.click(federatedTrigger);
        await waitFor(() =>
            expect(screen.getAllByRole('menuitem').map(item => item.textContent)).toEqual(['View Details', 'Edit Configuration']),
        );
    });

    it('navigates "View Analytics" to the API-filtered observability dashboard', async () => {
        const user = userEvent.setup();
        renderTable({ apis: [makeApi()] });
        await user.click(screen.getByRole('button', { name: 'API actions' }));
        await user.click(await screen.findByText('View Analytics'));
        expect(mockNavigate).toHaveBeenCalledWith(expect.stringContaining('../observe/dashboards/http-proxy-overview?'));
    });

    // Counterpart to the all-empty federated row asserted in the Owner cell suite: every dash below is
    // keyed on an absent value, so a cell renderer switched to an `isFederatedApiListItem` guard would
    // blank these columns for a row that does carry them while every empty-value case stayed green.
    it('renders each data column of a federated row from the value it carries, never the em dash', () => {
        renderTable({
            apis: [
                makeFederatedApi({
                    state: 'STARTED',
                    deploymentState: 'DEPLOYED',
                    listeners: [{ type: 'HTTP', paths: [{ path: '/orders' }] }],
                    tags: ['internal'],
                    primaryOwner: { displayName: 'Jane Doe' },
                }),
            ],
        });
        const [, dataRow] = screen.getAllByRole('row');
        const dataColumns = ['Runtime Status', 'Sync Status', 'Access', 'Sharding Tags', 'Owner'];

        expect(dataColumns.map(header => cellUnderHeader(dataRow, header).textContent)).toEqual([
            'Started',
            'In sync',
            '/orders',
            'internal',
            'Jane Doe',
        ]);
    });

    // Sync Status and Owner each have a two-row case of their own; the other three empty-value columns
    // are only ever rendered one row at a time above, so a guard hoisted out of the per-row cell into the
    // column definition would blank them for the row that does carry the value and stay green everywhere else.
    it('dashes only the federated row missing each value when a populated federated row is listed alongside', () => {
        renderTable({
            apis: [
                makeFederatedApi({ id: 'federated-empty', name: 'Orders API' }),
                makeFederatedApi({
                    id: 'federated-populated',
                    name: 'Billing API',
                    state: 'STARTED',
                    listeners: [{ type: 'HTTP', paths: [{ path: '/billing' }] }],
                    tags: ['internal'],
                }),
            ],
        });
        const [, emptyRow, populatedRow] = screen.getAllByRole('row');
        const columns = ['Runtime Status', 'Access', 'Sharding Tags'];

        expect(columns.map(header => cellUnderHeader(emptyRow, header).textContent)).toEqual(['—', '—', '—']);
        expect(columns.map(header => cellUnderHeader(populatedRow, header).textContent)).toEqual(['Started', '/billing', 'internal']);
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

        // `FederatedApiEntity.getState()` returns null unconditionally (FederatedApiEntity.java:107),
        // so the state reaches the client either as an absent key or as an explicit JSON null.
        const nullState = null as unknown as ApiState;

        it.each<[string, Partial<ApiListItem>]>([
            ['absent', {}],
            ['null', { state: nullState }],
        ])('shows the em dash and no status badge when the state is %s', (_scenario, overrides) => {
            renderTable({ apis: [makeFederatedApi(overrides)] });
            const [, dataRow] = screen.getAllByRole('row');
            const statusCell = cellUnderHeader(dataRow, 'Runtime Status');
            expect(statusCell.textContent).toBe('—');
            expect(within(statusCell).queryByText(/Started|Stopped|Closed/)).toBeNull();
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

        // A federated API is never deployed to a gateway, so `ApiMapper` drops the computed
        // deployment state for it (ApiMapper.java:152) — and the nullable property can reach the
        // client either as an absent key or as an explicit JSON null.
        const nullDeploymentState = null as unknown as ApiDeploymentState;

        it.each<[string, ApiListItem]>([
            ['absent on a federated row', makeFederatedApi()],
            ['null on a federated row', makeFederatedApi({ deploymentState: nullDeploymentState })],
            // The dash is keyed on the missing value, not on the row's kind — a guard on
            // `definitionVersion` would leave this row claiming "In sync" for a state it never received.
            ['absent on a natively-managed row', makeApi({ originContext: { origin: 'MANAGEMENT' } })],
        ])('shows the em dash and no "In sync" badge when the deployment state is %s', (_scenario, api) => {
            renderTable({ apis: [api] });
            const [, dataRow] = screen.getAllByRole('row');
            const syncCell = cellUnderHeader(dataRow, 'Sync Status');
            expect(syncCell.textContent).toBe('—');
            expect(within(syncCell).queryByText('In sync')).toBeNull();
        });

        // Only a cast can reach the default branch, since the value is outside the ApiDeploymentState
        // union — so nothing else would catch a refactor that restored the old "In sync" fallback.
        it('shows the em dash when the deployment state is outside the known union', () => {
            const fallbackWarning = jest.spyOn(console, 'warn').mockImplementation(() => {});
            const unknownDeploymentState = 'ARCHIVED' as unknown as ApiDeploymentState;

            renderTable({ apis: [makeFederatedApi({ deploymentState: unknownDeploymentState })] });

            const [, dataRow] = screen.getAllByRole('row');
            const syncCell = cellUnderHeader(dataRow, 'Sync Status');
            expect(syncCell.textContent).toBe('—');
            expect(within(syncCell).queryByText('In sync')).toBeNull();
            fallbackWarning.mockRestore();
        });

        // Federated and natively-managed APIs now arrive in the same response, so the dash has to be
        // decided per row: a guard hoisted to the column would blank every row once one lacked a state.
        it('dashes only the row missing a deployment state when both kinds are listed together', () => {
            renderTable({
                apis: [
                    makeFederatedApi({ id: 'federated', name: 'Orders API' }),
                    makeApi({ id: 'native', name: 'Payments API', originContext: { origin: 'MANAGEMENT' }, deploymentState: 'DEPLOYED' }),
                ],
            });
            const [, federatedRow, nativeRow] = screen.getAllByRole('row');
            expect(cellUnderHeader(federatedRow, 'Sync Status').textContent).toBe('—');
            expect(cellUnderHeader(nativeRow, 'Sync Status').textContent).toBe('In sync');
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
            // The cast models that off-contract payload, which ApiListOriginContext rightly rejects.
            [0, 'a row carrying a provider but no origin discriminator', { originContext: { provider: 'solace' } as ApiListOriginContext }],
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
            const originCell = cellUnderHeader(dataRow, 'Origin');
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
            ['spelled as the Solace display name rather than as its map key', 'Solace'],
            ['spelled in uppercase rather than as its lowercase map key', 'SOLACE'],
            ['near-missing the aws-api-gateway key a loosened match would absorb', 'aws-apigateway'],
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

    describe('Access cell', () => {
        it('renders access path when listener has an HTTP path', () => {
            renderTable({ apis: [makeApi({ listeners: [{ type: 'HTTP', paths: [{ path: '/my-api' }] }] })] });
            const [, dataRow] = screen.getAllByRole('row');
            const accessCell = cellUnderHeader(dataRow, 'Access');
            expect(accessCell.textContent).toBe('/my-api');
            expect(accessCell.querySelector('.font-mono')).not.toBeNull();
        });

        it.each<[string, Partial<ApiListItem>]>([
            ['absent', {}],
            ['empty', { listeners: [] }],
            // A naive `listeners?.length` guard would read a TCP-only API as having an access path.
            ['carrying no HTTP entry', { listeners: [{ type: 'TCP', host: 'tcp.example.com', port: 4082 }] }],
        ])('shows the em dash and no path badge when listeners are %s', (_scenario, overrides) => {
            renderTable({ apis: [makeFederatedApi(overrides)] });
            const [, dataRow] = screen.getAllByRole('row');
            const accessCell = cellUnderHeader(dataRow, 'Access');
            expect(accessCell.textContent).toBe('—');
            expect(accessCell.querySelector('.font-mono')).toBeNull();
        });
    });

    describe('Sharding Tags cell', () => {
        it('renders the first tag and an "N more" badge when tags are present', () => {
            renderTable({ apis: [makeApi({ tags: ['internal', 'edge'] })] });
            const [, dataRow] = screen.getAllByRole('row');
            const tagsCell = cellUnderHeader(dataRow, 'Sharding Tags');
            expect(within(tagsCell).queryByText('edge')).not.toBeNull();
            expect(within(tagsCell).queryByText('1 more')).not.toBeNull();
        });

        it.each<[string, Partial<ApiListItem>]>([
            ['absent', {}],
            ['empty', { tags: [] }],
        ])('shows the em dash and no "N more" badge when tags are %s', (_scenario, overrides) => {
            renderTable({ apis: [makeFederatedApi(overrides)] });
            const [, dataRow] = screen.getAllByRole('row');
            const tagsCell = cellUnderHeader(dataRow, 'Sharding Tags');
            expect(tagsCell.textContent).toBe('—');
            expect(within(tagsCell).queryByText(/more$/)).toBeNull();
        });
    });

    describe('Owner cell', () => {
        it('renders owner display name', () => {
            renderTable({ apis: [makeApi({ primaryOwner: { displayName: 'Jane Doe' } })] });
            const [, dataRow] = screen.getAllByRole('row');
            expect(cellUnderHeader(dataRow, 'Owner').textContent).toBe('Jane Doe');
        });

        it('keeps the owner of a federated row whose every other data column is empty', () => {
            renderTable({
                apis: [
                    makeFederatedApi({ id: 'federated-unowned', name: 'Orders API' }),
                    makeFederatedApi({
                        id: 'federated-owned',
                        name: 'Billing API',
                        primaryOwner: { displayName: 'Jane Doe' },
                    }),
                ],
            });
            const [, unownedRow, ownedRow] = screen.getAllByRole('row');
            const emptyColumns = ['Runtime Status', 'Sync Status', 'Access', 'Sharding Tags'];
            expect(emptyColumns.map(header => cellUnderHeader(ownedRow, header).textContent)).toEqual(['—', '—', '—', '—']);
            expect(cellUnderHeader(unownedRow, 'Owner').textContent).toBe('—');
            expect(cellUnderHeader(ownedRow, 'Owner').textContent).toBe('Jane Doe');
        });
    });
});
