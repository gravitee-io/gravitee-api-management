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
import { fireEvent, render, screen } from '@testing-library/react';

import { SubscriptionApproveDialog } from './SubscriptionApproveDialog';
import { useCustomApiKeyEnabled } from '../../../../hooks/usePlanSecuritySettings';
import { verifyCustomApiKey } from '../../../../services/subscriptions';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('../../../../hooks/usePlanSecuritySettings', () => ({
    useCustomApiKeyEnabled: jest.fn(() => true),
}));

jest.mock('../../../../services/subscriptions', () => ({
    verifyCustomApiKey: jest.fn(() => Promise.resolve({ ok: true })),
}));

const mockVerify = verifyCustomApiKey as jest.Mock;
const mockCustomApiKey = useCustomApiKeyEnabled as jest.Mock;

beforeAll(() => {
    global.ResizeObserver = class ResizeObserver {
        observe() {}
        unobserve() {}
        disconnect() {}
    } as typeof ResizeObserver;
});

function renderDialog(onConfirm = jest.fn()) {
    return render(
        <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
            <SubscriptionApproveDialog
                open
                ctx={{ type: 'api', entityId: 'api-1' }}
                applicationId="app-1"
                isApiKeyPlan
                isPending={false}
                onConfirm={onConfirm}
                onClose={jest.fn()}
            />
        </QueryClientProvider>,
    );
}

describe('SubscriptionApproveDialog', () => {
    beforeEach(() => {
        mockCustomApiKey.mockReturnValue(true);
        mockVerify.mockResolvedValue({ ok: true });
    });

    it('approves without a custom key', () => {
        const onConfirm = jest.fn();
        renderDialog(onConfirm);
        fireEvent.click(screen.getByRole('button', { name: 'Approve' }));
        expect(onConfirm).toHaveBeenCalledWith({});
        expect(mockVerify).not.toHaveBeenCalled();
    });

    it('blocks approve when the custom key is not valid', () => {
        const onConfirm = jest.fn();
        renderDialog(onConfirm);
        fireEvent.change(screen.getByLabelText('Custom API key'), { target: { value: 'nope' } });

        expect(screen.getByText('Must be longer than 8 characters')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Approve' })).toBeDisabled();
        fireEvent.click(screen.getByRole('button', { name: 'Approve' }));
        expect(onConfirm).not.toHaveBeenCalled();
    });
});
