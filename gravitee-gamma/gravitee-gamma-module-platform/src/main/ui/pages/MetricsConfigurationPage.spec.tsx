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
import { createHttpMetricDefinitionsSource, type MetricDefinitionEntry, ObservabilityHttpError } from '@gravitee/gamma-lib-observability';
import type * as ObservabilityLib from '@gravitee/gamma-lib-observability';
import { gammaConsoleHttpOptions } from '@gravitee/gamma-ui-shared/api';
import { useObservabilityBaseUrl } from '@gravitee/gamma-ui-shared/observability';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { MemoryRouter } from 'react-router-dom';

import { useEnvironment, useHasPermission } from '@gravitee/gamma-modules-sdk';

import { MetricsConfigurationPage } from './MetricsConfigurationPage';
import { createInMemoryMetricDefinitionsSource } from '../features/metrics-configuration/testing/inMemoryMetricDefinitionsSource';
import { retryTransientRequest } from '../shared/api/queryRetry';
import { installFormActionTestEnvironment } from '../shared/testing/formAction';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
    useHasPermission: jest.fn(),
}));

jest.mock('@gravitee/gamma-ui-shared/observability', () => ({
    useObservabilityBaseUrl: jest.fn(),
}));

jest.mock('@gravitee/gamma-lib-observability', () => ({
    ...jest.requireActual<typeof ObservabilityLib>('@gravitee/gamma-lib-observability'),
    createHttpMetricDefinitionsSource: jest.fn(),
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseObservabilityBaseUrl = jest.mocked(useObservabilityBaseUrl);
const mockCreateHttpMetricDefinitionsSource = jest.mocked(createHttpMetricDefinitionsSource);

const BASE_URL = '/gamma/organizations/org-1/environments/env-1';

const BUILT_IN: MetricDefinitionEntry = {
    name: 'mcp-proxy.tool',
    label: 'Tool',
    type: 'keyword',
    apiTypes: ['MCP'],
    origin: 'BUILT_IN',
    rawKey: 'keyword_mcp-proxy_tool',
    valueType: 'KEYWORD',
    lastSeen: '2026-11-04T09:00:00Z',
    exposed: true,
};

const DECLARED: MetricDefinitionEntry = {
    name: 'additional-metrics.double_checkout_basket-value',
    label: 'Basket value',
    description: 'Value of the basket at checkout',
    unit: '€',
    type: 'number',
    apiTypes: [],
    origin: 'DECLARED',
    rawKey: 'double_checkout_basket-value',
    valueType: 'DOUBLE',
    exposed: true,
};

const DISCOVERED: MetricDefinitionEntry = {
    name: 'additional-metrics.keyword_checkout_channel',
    label: 'Checkout channel',
    type: 'keyword',
    apiTypes: [],
    origin: 'DISCOVERED',
    rawKey: 'keyword_checkout_channel',
    valueType: 'KEYWORD',
    lastSeen: '2026-11-04T08:00:00Z',
    exposed: true,
};

function baseUrlKnown(): ReturnType<typeof useObservabilityBaseUrl> {
    return { baseUrl: BASE_URL, isLoading: false, isError: false, error: null };
}

/** Under the module's own QueryClient defaults (AppRoutes), which retry a failed mutation twice. */
function renderPage() {
    const queryClient = new QueryClient({
        defaultOptions: { queries: { retry: retryTransientRequest }, mutations: { retry: retryTransientRequest } },
    });
    function Wrapper({ children }: { children: ReactNode }) {
        return (
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={['/environments/env-1/platform/environment/metrics-configuration']}>{children}</MemoryRouter>
            </QueryClientProvider>
        );
    }
    return render(<MetricsConfigurationPage />, { wrapper: Wrapper });
}

function neverAnswering(): ReturnType<typeof createInMemoryMetricDefinitionsSource> {
    return {
        ...createInMemoryMetricDefinitionsSource([]),
        list: () => new Promise(() => undefined),
    };
}

function rowOf(label: string): HTMLElement {
    const row = screen.getByText(label).closest('tr');
    if (!row) throw new Error(`No table row carries "${label}"`);
    return row;
}

describe('MetricsConfigurationPage', () => {
    let restoreTestEnvironment: () => void;

    beforeAll(() => {
        restoreTestEnvironment = installFormActionTestEnvironment();
        // The sheets' selects are Radix ones: they need the pointer-capture API jsdom lacks.
        Element.prototype.hasPointerCapture = jest.fn();
        Element.prototype.setPointerCapture = jest.fn();
        Element.prototype.releasePointerCapture = jest.fn();
        Element.prototype.scrollIntoView = jest.fn();
    });

    afterAll(() => {
        restoreTestEnvironment();
    });

    beforeEach(() => {
        mockUseEnvironment.mockReturnValue({ id: 'env-1' });
        mockUseHasPermission.mockReturnValue(true);
        mockUseObservabilityBaseUrl.mockReturnValue(baseUrlKnown());
        mockCreateHttpMetricDefinitionsSource.mockImplementation(() =>
            createInMemoryMetricDefinitionsSource([BUILT_IN, DECLARED, DISCOVERED]),
        );
    });

    it('renders nothing, and builds no source, until the host reports the environment', () => {
        mockUseEnvironment.mockReturnValue(undefined);
        const { container } = renderPage();
        expect(container).toBeEmptyDOMElement();
        expect(mockUseObservabilityBaseUrl).not.toHaveBeenCalled();
        expect(mockCreateHttpMetricDefinitionsSource).not.toHaveBeenCalled();
    });

    it('renders nothing, and builds no source, until the base URL is known', () => {
        mockUseObservabilityBaseUrl.mockReturnValue({ baseUrl: undefined, isLoading: true, isError: false, error: null });
        const { container } = renderPage();
        expect(container).toBeEmptyDOMElement();
        expect(mockCreateHttpMetricDefinitionsSource).not.toHaveBeenCalled();
    });

    it('says so when the base URL could not be resolved', () => {
        mockUseObservabilityBaseUrl.mockReturnValue({ baseUrl: undefined, isLoading: false, isError: true, error: new Error('bootstrap') });
        renderPage();
        // The library's own failure state, as the catalog shows for a failed list.
        expect(screen.getByText("Couldn't load the environment configuration")).not.toBeNull();
        expect(screen.getByRole('button', { name: 'View details' })).not.toBeNull();
        expect(mockCreateHttpMetricDefinitionsSource).not.toHaveBeenCalled();
    });

    it('mounts the catalog on the environment base URL with the console HTTP options', async () => {
        renderPage();

        expect(mockCreateHttpMetricDefinitionsSource).toHaveBeenCalledWith(BASE_URL, gammaConsoleHttpOptions);
        expect(await screen.findByText('Basket value')).not.toBeNull();
        expect(screen.getByText('Tool')).not.toBeNull();
        expect(screen.getByText('Checkout channel')).not.toBeNull();
        // One title and one description: the catalog's own, with no page header above them.
        expect(screen.getAllByRole('heading').map(heading => heading.textContent)).toEqual(['Additional metrics']);
        expect(screen.queryByText(/Fields discovered in this environment/)).toBeNull();
        // The list resolving re-rendered the page: the source is built once per base URL, not per render.
        expect(mockCreateHttpMetricDefinitionsSource).toHaveBeenCalledTimes(1);
    });

    it('remounts the catalog when the host switches environment', async () => {
        const baseUrlOf = (environmentId: string) => `/gamma/organizations/org-1/environments/${environmentId}`;
        mockUseEnvironment.mockImplementation(() => ({ id: 'env-1' }));
        mockUseObservabilityBaseUrl.mockImplementation(() => ({ ...baseUrlKnown(), baseUrl: baseUrlOf('env-1') }));
        mockCreateHttpMetricDefinitionsSource.mockImplementation(baseUrl =>
            baseUrl === baseUrlOf('env-1') ? createInMemoryMetricDefinitionsSource([BUILT_IN, DECLARED, DISCOVERED]) : neverAnswering(),
        );
        const { rerender } = renderPage();
        await screen.findByText('Basket value');

        mockUseEnvironment.mockImplementation(() => ({ id: 'env-2' }));
        mockUseObservabilityBaseUrl.mockImplementation(() => ({ ...baseUrlKnown(), baseUrl: baseUrlOf('env-2') }));
        rerender(<MetricsConfigurationPage />);

        // env-2 has not answered: the catalog must not keep showing env-1's rows meanwhile.
        await waitFor(() => expect(screen.queryByText('Basket value')).toBeNull());
        expect(mockCreateHttpMetricDefinitionsSource).toHaveBeenLastCalledWith(baseUrlOf('env-2'), gammaConsoleHttpOptions);
    });

    it('has no Save bar: the catalog saves per sheet', async () => {
        renderPage();
        await screen.findByText('Basket value');
        expect(screen.queryByRole('button', { name: /Save changes/i })).toBeNull();
        expect(screen.queryByRole('button', { name: /Discard/i })).toBeNull();
    });

    it('offers the edit controls to a holder of environment-settings-u', async () => {
        renderPage();
        await screen.findByText('Basket value');
        expect(mockUseHasPermission).toHaveBeenCalledWith({ anyOf: ['environment-settings-u'] });
        expect(screen.getByRole('button', { name: 'Declare a metric' })).not.toBeNull();
        expect(screen.getByRole('button', { name: 'Edit Basket value' })).not.toBeNull();
    });

    it('is read-only without environment-settings-u', async () => {
        mockUseHasPermission.mockReturnValue(false);
        renderPage();
        await screen.findByText('Basket value');
        expect(screen.queryByRole('button', { name: 'Declare a metric' })).toBeNull();
        expect(screen.queryByRole('button', { name: /^Edit / })).toBeNull();
    });

    it('declares a decimal metric with a description and a custom unit, and lists it as declared without data', async () => {
        mockCreateHttpMetricDefinitionsSource.mockImplementation(() => createInMemoryMetricDefinitionsSource([BUILT_IN, DISCOVERED]));
        const user = userEvent.setup();
        renderPage();
        await screen.findByText('Checkout channel');

        await user.click(screen.getByRole('button', { name: 'Declare a metric' }));
        const sheet = await screen.findByRole('dialog', { name: 'Declare a metric' });
        await user.click(within(sheet).getByRole('combobox', { name: 'Type' }));
        await user.click(await screen.findByRole('option', { name: 'double: decimal number' }));
        await user.type(within(sheet).getByLabelText('Name'), 'checkout_basket-value');
        await user.type(within(sheet).getByLabelText('Label'), 'Basket value');
        await user.type(within(sheet).getByLabelText('Description'), 'Value of the basket at checkout');
        await user.click(within(sheet).getByRole('combobox', { name: 'Unit' }));
        await user.click(await screen.findByRole('option', { name: 'Custom suffix' }));
        await user.type(within(sheet).getByLabelText('Custom suffix'), '€');
        await user.click(within(sheet).getByRole('button', { name: 'Declare' }));

        await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Declare a metric' })).toBeNull());
        const row = await waitFor(() => rowOf('Basket value'));
        expect(within(row).getByText('Declared')).not.toBeNull();
        expect(within(row).getByText('double_checkout_basket-value')).not.toBeNull();
        expect(within(row).getByText('No data in the last 30 days')).not.toBeNull();
        expect(within(row).getByText('€')).not.toBeNull();
    });

    it('does not retry a failed declaration: the sheet reports it at once', async () => {
        const failed = new ObservabilityHttpError(503, 'HTTP 503 on POST metric-definitions', 'POST metric-definitions');
        const declare = jest.fn().mockRejectedValue(failed);
        mockCreateHttpMetricDefinitionsSource.mockImplementation(() => ({
            ...createInMemoryMetricDefinitionsSource([BUILT_IN]),
            declare,
        }));
        const user = userEvent.setup();
        renderPage();
        await screen.findByText('Tool');

        await user.click(screen.getByRole('button', { name: 'Declare a metric' }));
        const sheet = await screen.findByRole('dialog', { name: 'Declare a metric' });
        await user.type(within(sheet).getByLabelText('Name'), 'checkout_channel-count');
        await user.click(within(sheet).getByRole('button', { name: 'Declare' }));

        // Under the module's defaults the failure would surface only after two retries, seconds later.
        expect(await within(sheet).findByRole('alert')).not.toBeNull();
        expect(declare).toHaveBeenCalledTimes(1);
        expect(screen.getByRole('dialog', { name: 'Declare a metric' })).not.toBeNull();
    });

    it('retries a transient list failure: a hiccup never leaves the catalog empty', async () => {
        const source = createInMemoryMetricDefinitionsSource([BUILT_IN, DECLARED, DISCOVERED]);
        const list = jest
            .fn()
            .mockRejectedValueOnce(new ObservabilityHttpError(503, 'HTTP 503 on GET metric-definitions', 'GET metric-definitions'))
            .mockImplementation(() => source.list());
        mockCreateHttpMetricDefinitionsSource.mockImplementation(() => ({ ...source, list }));
        renderPage();

        // react-query waits a second before the retry: longer than the default timeout.
        expect(await screen.findByText('Basket value', {}, { timeout: 4000 })).not.toBeNull();
        expect(list).toHaveBeenCalledTimes(2);
    });

    it("gives a failing list the module's two retries, then reports it", async () => {
        const list = jest
            .fn()
            .mockRejectedValue(new ObservabilityHttpError(503, 'HTTP 503 on GET metric-definitions', 'GET metric-definitions'));
        mockCreateHttpMetricDefinitionsSource.mockImplementation(() => ({ ...createInMemoryMetricDefinitionsSource([]), list }));
        renderPage();

        // Two retries, a second then two seconds later; react-query's defaults would ask a third time and report after seven.
        expect(await screen.findByText("Couldn't load additional metrics", {}, { timeout: 4500 })).not.toBeNull();
        expect(list).toHaveBeenCalledTimes(3);
    });

    it('edits a declared record: label and exposure', async () => {
        const user = userEvent.setup();
        renderPage();
        await screen.findByText('Basket value');

        await user.click(screen.getByRole('button', { name: 'Edit Basket value' }));
        const sheet = await screen.findByRole('dialog', { name: 'Edit metric' });
        const label = within(sheet).getByLabelText('Label');
        await user.clear(label);
        await user.type(label, 'Basket total');
        await user.click(within(sheet).getByRole('switch', { name: 'Exposed' }));
        await user.click(within(sheet).getByRole('button', { name: 'Save' }));

        await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Edit metric' })).toBeNull());
        const row = await waitFor(() => rowOf('Basket total'));
        expect(within(row).getByText('Hidden')).not.toBeNull();
        expect(screen.queryByText('Basket value')).toBeNull();
    });

    it('deletes a declared record', async () => {
        const user = userEvent.setup();
        renderPage();
        await screen.findByText('Basket value');

        await user.click(screen.getByRole('button', { name: 'Edit Basket value' }));
        const sheet = await screen.findByRole('dialog', { name: 'Edit metric' });
        await user.click(within(sheet).getByRole('button', { name: 'Delete record' }));
        const confirmation = await screen.findByRole('dialog', { name: 'Delete this record?' });
        await user.click(within(confirmation).getByRole('button', { name: 'Delete record' }));

        await waitFor(() => expect(screen.queryByText('Basket value')).toBeNull());
        expect(screen.getByText('Tool')).not.toBeNull();
        expect(screen.getByText('Checkout channel')).not.toBeNull();
    });
});
