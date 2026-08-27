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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';

jest.mock('../../services/policyStudioService', () => ({
    listEntrypointPlugins: jest.fn(),
}));

import { DetailsStep } from './DetailsStep';
import { listEntrypointPlugins } from '../../services/policyStudioService';
import { ApiCreationProvider, useApiCreation } from '../../store/apiCreationStore';
import type { ConnectorPlugin } from '../../types/policyStudio';

const mockListEntrypointPlugins = jest.mocked(listEntrypointPlugins);

function plugin(id: string, overrides: Partial<ConnectorPlugin> = {}): ConnectorPlugin {
    return { id, name: id, supportedModes: [], deployed: true, ...overrides };
}

function CurrentProtocol() {
    const { state } = useApiCreation();
    return <span data-testid="current-protocol">{state.form.protocol}</span>;
}

function renderStep() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <ApiCreationProvider>
                <DetailsStep />
                <CurrentProtocol />
            </ApiCreationProvider>
        </QueryClientProvider>,
    );
}

describe('DetailsStep — proxy kind gating', () => {
    afterEach(() => jest.clearAllMocks());

    it('keeps both options selectable when both plugins are deployed', async () => {
        mockListEntrypointPlugins.mockResolvedValue([plugin('http-proxy'), plugin('tcp-proxy')]);
        renderStep();

        await waitFor(() => expect(mockListEntrypointPlugins).toHaveBeenCalled());

        const tcpOption = await screen.findByRole('radio', { name: /tcp proxy/i });
        expect(tcpOption).not.toBeDisabled();

        fireEvent.click(tcpOption);
        expect(screen.getByTestId('current-protocol')).toHaveTextContent('TCP');
    });

    it('keeps options selectable while the plugin list is still loading (fail open)', () => {
        mockListEntrypointPlugins.mockReturnValue(new Promise(() => {}));
        renderStep();

        expect(screen.getByRole('radio', { name: /tcp proxy/i })).not.toBeDisabled();
    });

    it('disables the TCP Proxy option and shows why when the tcp-proxy plugin is not deployed', async () => {
        mockListEntrypointPlugins.mockResolvedValue([plugin('http-proxy'), plugin('tcp-proxy', { deployed: false })]);
        renderStep();

        const tcpOption = await screen.findByRole('radio', { name: /tcp proxy/i });
        await waitFor(() => expect(tcpOption).toBeDisabled());
        expect(screen.getByText(/not available/i)).toBeInTheDocument();

        fireEvent.click(tcpOption);
        expect(screen.getByTestId('current-protocol')).toHaveTextContent('HTTP');
    });

    it('fails closed once loaded: disables an option whose plugin is missing from the response entirely', async () => {
        // Only http-proxy comes back — tcp-proxy isn't in the list at all, not just deployed: false.
        mockListEntrypointPlugins.mockResolvedValue([plugin('http-proxy')]);
        renderStep();

        const tcpOption = await screen.findByRole('radio', { name: /tcp proxy/i });
        await waitFor(() => expect(tcpOption).toBeDisabled());
    });
});
