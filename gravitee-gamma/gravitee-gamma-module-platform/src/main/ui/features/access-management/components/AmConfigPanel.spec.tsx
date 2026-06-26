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
import { fireEvent, render, screen, waitFor } from '@testing-library/react';

import { AmConfigPanel } from './AmConfigPanel';
import { resolveOrganizationId } from '../../../shared/api/apimClient';
import { getAmConnection, isAmUnavailable, listEnvironments, saveAmConnection, testAmConnection } from '../services/amManagement';
import { loadAmConfig, saveAmConfig } from '../utils/amConfig';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'gravitee-env-1' })),
}));

jest.mock('../../../shared/api/apimClient', () => ({
    resolveOrganizationId: jest.fn(),
}));

jest.mock('../services/amManagement', () => ({
    getAmConnection: jest.fn(),
    isAmUnavailable: jest.fn(() => false),
    listDomains: jest.fn(),
    listDomainEntrypoints: jest.fn(),
    listEnvironments: jest.fn(),
    saveAmConnection: jest.fn(),
    testAmConnection: jest.fn(),
    getDomain: jest.fn(),
}));

jest.mock('../utils/amConfig', () => ({
    loadAmConfig: jest.fn(),
    saveAmConfig: jest.fn(),
}));

const mockGetAmConnection = jest.mocked(getAmConnection);
const mockTestAmConnection = jest.mocked(testAmConnection);
const mockSaveAmConnection = jest.mocked(saveAmConnection);
const mockListEnvironments = jest.mocked(listEnvironments);

const STORED_CONNECTION = {
    baseUrl: 'https://am.example',
    hasAccessToken: true,
    amOrganizationId: 'DEFAULT',
    environmentId: null,
    defaultDomainId: null,
    defaultDomainHrid: null,
    gatewayUrl: null,
};

function org() {
    return screen.getByPlaceholderText('DEFAULT') as HTMLInputElement;
}

function saveButton() {
    return screen.getByRole('button', { name: 'Save' }) as HTMLButtonElement;
}

function renderPanel() {
    const onSaved = jest.fn();
    render(<AmConfigPanel onSaved={onSaved} />);
    return { onSaved };
}

describe('AmConfigPanel', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        jest.mocked(loadAmConfig).mockReturnValue({
            organizationId: 'apim-org',
            environmentId: '',
            domainId: '',
            graviteeEnvironmentId: 'gravitee-env-1',
        });
        jest.mocked(resolveOrganizationId).mockResolvedValue('apim-org');
        jest.mocked(isAmUnavailable).mockReturnValue(false);
        // A stored connection means the panel loads verified and lists environments; keep that empty.
        mockGetAmConnection.mockResolvedValue({ ...STORED_CONNECTION });
        mockListEnvironments.mockResolvedValue([]);
        jest.mocked(saveAmConfig).mockReturnValue(undefined);
    });

    it('pre-fills the AM organization with DEFAULT', async () => {
        renderPanel();

        await waitFor(() => expect(org().value).toBe('DEFAULT'));
    });

    it('disables Save when the organization is only whitespace', async () => {
        renderPanel();
        await waitFor(() => expect(org().value).toBe('DEFAULT'));

        fireEvent.change(org(), { target: { value: '   ' } });

        expect(saveButton().disabled).toBe(true);
    });

    it('trims the organization before persisting on Save', async () => {
        mockSaveAmConnection.mockResolvedValue({ ...STORED_CONNECTION });
        renderPanel();
        await waitFor(() => expect(org().value).toBe('DEFAULT'));

        fireEvent.change(org(), { target: { value: '  am-org  ' } });
        fireEvent.click(screen.getByRole('button', { name: 'Save' }));

        await waitFor(() => expect(mockSaveAmConnection).toHaveBeenCalled());
        expect(mockSaveAmConnection.mock.calls[0][1]).toEqual(expect.objectContaining({ amOrganizationId: 'am-org' }));
    });

    it('passes an abort signal to the verify call and reports success', async () => {
        mockTestAmConnection.mockResolvedValue({ ok: true });
        mockSaveAmConnection.mockResolvedValue({ ...STORED_CONNECTION });
        renderPanel();
        await waitFor(() => expect(org().value).toBe('DEFAULT'));

        fireEvent.click(screen.getByRole('button', { name: 'Verify & Load' }));

        await screen.findByText('Connection succeeded');
        expect(mockTestAmConnection.mock.calls[0][2]).toBeInstanceOf(AbortSignal);
    });

    it('aborts the in-flight verify and shows Cancelled when Cancel is clicked', async () => {
        // Reject only once the request is aborted, so the Cancel button drives the outcome.
        mockTestAmConnection.mockImplementation(
            (_cfg, _req, signal?: AbortSignal) =>
                new Promise((_resolve, reject) => {
                    signal?.addEventListener('abort', () => reject(new DOMException('aborted', 'AbortError')));
                }),
        );
        renderPanel();
        await waitFor(() => expect(org().value).toBe('DEFAULT'));

        fireEvent.click(screen.getByRole('button', { name: 'Verify & Load' }));
        fireEvent.click(await screen.findByRole('button', { name: 'Cancel' }));

        await screen.findByText('Cancelled');
        expect(mockSaveAmConnection).not.toHaveBeenCalled();
    });

    it('aborts an in-flight verify when a field is edited', async () => {
        let capturedSignal: AbortSignal | undefined;
        mockTestAmConnection.mockImplementation((_cfg, _req, signal?: AbortSignal) => {
            capturedSignal = signal;
            return new Promise((_resolve, reject) => {
                signal?.addEventListener('abort', () => reject(new DOMException('aborted', 'AbortError')));
            });
        });
        renderPanel();
        await waitFor(() => expect(org().value).toBe('DEFAULT'));

        fireEvent.click(screen.getByRole('button', { name: 'Verify & Load' }));
        await waitFor(() => expect(capturedSignal).toBeDefined());
        fireEvent.change(org(), { target: { value: 'am-org' } });

        await waitFor(() => expect(capturedSignal?.aborted).toBe(true));
    });
});
