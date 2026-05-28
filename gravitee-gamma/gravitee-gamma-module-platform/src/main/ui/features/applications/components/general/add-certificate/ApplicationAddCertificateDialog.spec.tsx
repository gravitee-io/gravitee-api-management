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

import { ADD_CERTIFICATE_TEST_IDS } from './addCertificateTestIds';
import { ApplicationAddCertificateDialog } from './ApplicationAddCertificateDialog';
import { validateApplicationCertificate } from '../../../services/applicationDetail';
import type { ClientCertificate } from '../../../types/applicationCertificate';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'env-1' })),
}));

jest.mock('../../../services/applicationDetail', () => ({
    validateApplicationCertificate: jest.fn(),
}));

const mockValidate = jest.mocked(validateApplicationCertificate);

const VALID_PEM = '-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----';
const MOCK_VALIDATION_RESPONSE = {
    certificateExpiration: '2027-06-15T10:00:00.000Z',
    subject: 'CN=test',
    issuer: 'CN=issuer',
};

function renderDialog(certificates: ClientCertificate[] = []) {
    const onSubmit = jest.fn();
    render(
        <ApplicationAddCertificateDialog
            open
            onOpenChange={jest.fn()}
            applicationId="app-1"
            certificates={certificates}
            onSubmit={onSubmit}
            isSubmitting={false}
        />,
    );
    return { onSubmit };
}

async function fillUploadForm(name = 'my-cert', certificate = VALID_PEM) {
    fireEvent.change(screen.getByTestId(ADD_CERTIFICATE_TEST_IDS.nameInput), { target: { value: name } });
    fireEvent.change(screen.getByTestId(ADD_CERTIFICATE_TEST_IDS.pemInput), { target: { value: certificate } });
}

async function advanceToConfigureStep(name = 'my-cert', certificate = VALID_PEM) {
    await fillUploadForm(name, certificate);
    fireEvent.click(screen.getByTestId(ADD_CERTIFICATE_TEST_IDS.validateButton));
    await waitFor(() => expect(mockValidate).toHaveBeenCalled());
    await waitFor(() => expect(screen.getByTestId(ADD_CERTIFICATE_TEST_IDS.validationSuccessBanner)).toBeTruthy());
}

describe('ApplicationAddCertificateDialog', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockValidate.mockResolvedValue(MOCK_VALIDATION_RESPONSE);
    });

    describe('first certificate (no active certificates)', () => {
        it('renders name and certificate fields on the upload step', () => {
            renderDialog();
            expect(screen.getByTestId(ADD_CERTIFICATE_TEST_IDS.nameInput)).toBeTruthy();
            expect(screen.getByTestId(ADD_CERTIFICATE_TEST_IDS.pemInput)).toBeTruthy();
            expect(screen.queryByTestId(ADD_CERTIFICATE_TEST_IDS.gracePeriodInput)).toBeNull();
        });

        it('stays on upload when continue is clicked with an empty form', async () => {
            renderDialog();
            fireEvent.click(screen.getByTestId(ADD_CERTIFICATE_TEST_IDS.validateButton));
            expect(screen.getByText('Certificate name is required.')).toBeTruthy();
            expect(screen.getByText('Certificate is required.')).toBeTruthy();
            expect(mockValidate).not.toHaveBeenCalled();
        });

        it('shows validation error banner when validation fails', async () => {
            mockValidate.mockRejectedValueOnce(new Error('bad'));
            renderDialog();
            await fillUploadForm();
            fireEvent.click(screen.getByTestId(ADD_CERTIFICATE_TEST_IDS.validateButton));
            await waitFor(() => expect(screen.getByTestId(ADD_CERTIFICATE_TEST_IDS.validationErrorBanner)).toBeTruthy());
            expect(screen.getByText('Invalid certificate format')).toBeTruthy();
        });

        it('populates the certificate field when a PEM file is uploaded', async () => {
            renderDialog();
            const pemContent = '-----BEGIN CERTIFICATE-----\nfiletest\n-----END CERTIFICATE-----';
            const file = new File([pemContent], 'cert.pem', { type: 'application/x-pem-file' });
            const input = document.querySelector('input[type="file"]') as HTMLInputElement;
            fireEvent.change(input, { target: { files: [file] } });
            await waitFor(() => {
                expect((screen.getByTestId(ADD_CERTIFICATE_TEST_IDS.pemInput) as HTMLTextAreaElement).value).toBe(pemContent);
            });
        });

        it('submits with name and certificate after the confirm step', async () => {
            const { onSubmit } = renderDialog();
            await advanceToConfigureStep();
            fireEvent.click(screen.getByTestId(ADD_CERTIFICATE_TEST_IDS.continueButton));
            await waitFor(() => expect(screen.getByTestId(ADD_CERTIFICATE_TEST_IDS.submitButton)).toBeTruthy());
            fireEvent.click(screen.getByTestId(ADD_CERTIFICATE_TEST_IDS.submitButton));
            await waitFor(() => expect(onSubmit).toHaveBeenCalled());
            expect(onSubmit).toHaveBeenCalledWith(
                expect.objectContaining({
                    name: 'my-cert',
                    certificate: VALID_PEM,
                    endsAt: MOCK_VALIDATION_RESPONSE.certificateExpiration,
                }),
            );
        });

        it('shows the summary on the confirm step', async () => {
            renderDialog();
            await advanceToConfigureStep();
            fireEvent.click(screen.getByTestId(ADD_CERTIFICATE_TEST_IDS.continueButton));
            expect(screen.getByTestId(ADD_CERTIFICATE_TEST_IDS.summary)).toBeTruthy();
            expect(screen.getByTestId(ADD_CERTIFICATE_TEST_IDS.summaryName).textContent).toBe('my-cert');
            expect(screen.queryByTestId(ADD_CERTIFICATE_TEST_IDS.summaryGracePeriod)).toBeNull();
        });
    });

    describe('certificate rotation', () => {
        const activeCert: ClientCertificate = {
            id: 'cert-active',
            name: 'current',
            createdAt: '2024-01-01T00:00:00.000Z',
            status: 'ACTIVE',
            certificateExpiration: '2027-01-01T00:00:00.000Z',
        };

        it('shows grace period controls when an active certificate exists', async () => {
            renderDialog([activeCert]);
            await advanceToConfigureStep();
            expect(screen.getByTestId(ADD_CERTIFICATE_TEST_IDS.gracePeriodInput)).toBeTruthy();
            expect(screen.getByText(/Both certificates will remain active/i)).toBeTruthy();
        });
    });

    describe('upload revalidation', () => {
        it('returns to the upload step when the PEM changes after validation', async () => {
            renderDialog();
            await advanceToConfigureStep();
            fireEvent.click(screen.getByTestId(ADD_CERTIFICATE_TEST_IDS.previousButton));
            fireEvent.change(screen.getByTestId(ADD_CERTIFICATE_TEST_IDS.pemInput), {
                target: { value: `${VALID_PEM}\nchanged` },
            });
            expect(screen.getByTestId(ADD_CERTIFICATE_TEST_IDS.validateButton)).toBeTruthy();
            expect(screen.queryByTestId(ADD_CERTIFICATE_TEST_IDS.validationSuccessBanner)).toBeNull();
        });
    });
});
