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

import { ApplicationSharedApiKeysView } from './ApplicationSharedApiKeysView';
import type { ApplicationSubscriptionApiKeyRow } from '../../types/applicationSubscription';

const validKey: ApplicationSubscriptionApiKeyRow = {
    id: 'key-1',
    key: 'current-key-value',
    maskedKey: '••••••••alue',
    isValid: true,
    createdAt: 1,
};

const revokedKey: ApplicationSubscriptionApiKeyRow = {
    id: 'key-2',
    key: 'revoked-key-value',
    maskedKey: '••••••••lue2',
    isValid: false,
    createdAt: 1,
    endDate: 2,
};

function renderView(overrides: Partial<Parameters<typeof ApplicationSharedApiKeysView>[0]> = {}) {
    const onRenew = jest.fn();
    const onRevoke = jest.fn();
    render(
        <ApplicationSharedApiKeysView
            apiKeyMode="SHARED"
            apiKeys={[validKey]}
            isLoading={false}
            isError={false}
            canManage
            canUseCustomApiKey
            renewPending={false}
            revokePending={false}
            onRenew={onRenew}
            onRevoke={onRevoke}
            {...overrides}
        />,
    );
    return { onRenew, onRevoke };
}

describe('ApplicationSharedApiKeysView', () => {
    it('keeps Renew available when every key is revoked', () => {
        renderView({ apiKeys: [revokedKey] });

        expect(screen.getByRole('button', { name: 'Renew' })).toBeEnabled();
        expect(screen.queryByRole('button', { name: 'Revoke' })).toBeNull();
    });

    it('sorts valid keys above revoked keys by default', () => {
        renderView({ apiKeys: [revokedKey, validKey] });

        const rows = screen.getAllByRole('row');
        expect(rows[1]).toHaveTextContent('current-key-value');
        expect(rows[2]).toHaveTextContent('revoked-key-value');
    });

    it('renews with the chosen value from the dialog', () => {
        const { onRenew } = renderView();

        fireEvent.click(screen.getByRole('button', { name: 'Renew' }));
        fireEvent.change(screen.getByTestId('shared-api-key-renew-value'), { target: { value: '  chosen-key  ' } });
        fireEvent.click(screen.getByTestId('shared-api-key-renew-submit'));

        expect(onRenew).toHaveBeenCalledWith('chosen-key');
    });

    it('hides the value field when custom API keys are disabled', () => {
        renderView({ canUseCustomApiKey: false });

        fireEvent.click(screen.getByRole('button', { name: 'Renew' }));

        expect(screen.queryByTestId('shared-api-key-renew-value')).toBeNull();
        expect(screen.getByTestId('shared-api-key-renew-notice')).toBeInTheDocument();
    });

    it('shows the key value and a revoke action for a valid key', () => {
        renderView();

        expect(screen.getByText('current-key-value')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Revoke' })).toBeEnabled();
    });

    it('shows a load error instead of an empty list', () => {
        renderView({ apiKeys: [], isError: true });

        expect(screen.getByText('Unable to load API keys. Please try again.')).toBeInTheDocument();
        expect(screen.queryByText('No API keys')).toBeNull();
    });

    it('keeps the dialog open when renewal is rejected', async () => {
        const { onRenew } = renderView();
        onRenew.mockRejectedValue(new Error('This API key cannot be used'));

        fireEvent.click(screen.getByRole('button', { name: 'Renew' }));
        fireEvent.change(screen.getByTestId('shared-api-key-renew-value'), { target: { value: 'current-key' } });
        fireEvent.click(screen.getByTestId('shared-api-key-renew-submit'));

        await waitFor(() => expect(onRenew).toHaveBeenCalled());
        // Flush the rejection handler and the resulting React update before asserting the dialog stays open.
        await new Promise(resolve => setTimeout(resolve, 0));
        expect(screen.getByTestId('shared-api-key-renew-notice')).toBeInTheDocument();
    });

    it('does not offer shared keys for an exclusive application', () => {
        renderView({ apiKeyMode: 'EXCLUSIVE' });

        expect(screen.getByText(/only for an application that uses shared mode/i)).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Renew' })).toBeNull();
    });
});
