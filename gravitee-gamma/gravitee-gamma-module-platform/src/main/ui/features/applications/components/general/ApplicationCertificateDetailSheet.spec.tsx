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
import { fireEvent, render, screen } from '@testing-library/react';

import { ApplicationCertificateDetailSheet } from './ApplicationCertificateDetailSheet';
import type { ClientCertificate } from '../../types/applicationCertificate';
import { querySheetHeading } from '../test/sheetSpecHelpers';

const certificate: ClientCertificate = {
    id: 'cert-1',
    name: 'prod-client',
    createdAt: '2024-06-01T10:00:00.000Z',
    status: 'ACTIVE',
};

function renderSheet(cert: ClientCertificate | null = certificate) {
    const onClose = jest.fn();
    render(<ApplicationCertificateDetailSheet certificate={cert} onClose={onClose} />);
    return { onClose };
}

describe('ApplicationCertificateDetailSheet', () => {
    it('does not show sheet content when certificate is null', () => {
        renderSheet(null);
        expect(querySheetHeading('prod-client')).toBeNull();
    });

    it('shows certificate name as sheet title when certificate is set', () => {
        renderSheet(certificate);
        expect(screen.getByRole('heading', { name: 'prod-client' })).not.toBeNull();
    });

    it('invokes onClose when header close icon is clicked', () => {
        const { onClose } = renderSheet(certificate);
        const [headerClose] = screen.getAllByRole('button', { name: 'Close' });
        fireEvent.click(headerClose);
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('invokes onClose when footer Close is clicked', () => {
        const { onClose } = renderSheet(certificate);
        const [, footerClose] = screen.getAllByRole('button', { name: 'Close' });
        fireEvent.click(footerClose);
        expect(onClose).toHaveBeenCalledTimes(1);
    });
});
