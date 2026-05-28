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
import { act, renderHook, waitFor } from '@testing-library/react';

import { INVALID_CERTIFICATE_MESSAGE } from './addCertificateTypes';
import { useAddCertificateWizard } from './useAddCertificateWizard';
import { validateApplicationCertificate } from '../../../services/applicationDetail';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'env-1' })),
}));

jest.mock('../../../services/applicationDetail', () => ({
    validateApplicationCertificate: jest.fn(),
}));

const mockValidate = jest.mocked(validateApplicationCertificate);

describe('useAddCertificateWizard', () => {
    const onSubmit = jest.fn();

    beforeEach(() => {
        jest.clearAllMocks();
        mockValidate.mockResolvedValue({
            certificateExpiration: '2027-06-15T10:00:00.000Z',
            subject: 'CN=test',
            issuer: 'CN=issuer',
        });
    });

    it('does not advance when validation returns an invalid expiration', async () => {
        mockValidate.mockResolvedValueOnce({
            certificateExpiration: 'not-a-date',
            subject: 'CN=test',
            issuer: 'CN=issuer',
        });

        const { result } = renderHook(() =>
            useAddCertificateWizard({
                open: true,
                applicationId: 'app-1',
                certificates: [],
                onSubmit,
            }),
        );

        act(() => {
            result.current.handleNameChange('my-cert');
            result.current.handleCertificateChange('-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----');
        });

        await act(async () => {
            await result.current.handleValidateAndContinue();
        });

        expect(result.current.stepIndex).toBe(0);
        expect(result.current.validationError).toBe(INVALID_CERTIFICATE_MESSAGE);
    });

    it('requires grace period end when rotating certificates', async () => {
        const { result } = renderHook(() =>
            useAddCertificateWizard({
                open: true,
                applicationId: 'app-1',
                certificates: [
                    {
                        id: 'active-1',
                        name: 'current',
                        createdAt: '2024-01-01T00:00:00.000Z',
                        status: 'ACTIVE',
                    },
                ],
                onSubmit,
            }),
        );

        act(() => {
            result.current.handleNameChange('next-cert');
            result.current.handleCertificateChange('-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----');
        });

        await act(async () => {
            await result.current.handleValidateAndContinue();
        });

        await waitFor(() => expect(result.current.stepIndex).toBe(1));

        act(() => {
            result.current.setGracePeriodEnd(undefined);
        });

        expect(result.current.configureErrors.gracePeriodEnd).toBeTruthy();
        expect(result.current.canContinueConfigure).toBe(false);
    });
});
