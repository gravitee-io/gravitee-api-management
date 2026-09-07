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
import { TargetsPanel } from '@gravitee/gamma-lib-observability';
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { render, screen } from '@testing-library/react';
import type { ReactNode } from 'react';

import { ApiTargetsPage } from './ApiTargetsPage';
import { ApiDetailContext } from '../../../context/ApiDetailContext';
import type { ApiDetailDto } from '../../../types';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useHasPermission: jest.fn(() => true),
}));

jest.mock('@gravitee/gamma-lib-observability', () => ({
    TargetsPanel: jest.fn(() => <div data-testid="targets-panel" />),
}));

jest.mock('../../../components/targets/ApiTargetsProvider', () => ({
    ApiTargetsProvider: ({ children }: { children: ReactNode }) => <div data-testid="targets-provider">{children}</div>,
}));

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockTargetsPanel = TargetsPanel as unknown as jest.Mock;

const API = { id: 'api-1', name: 'Orders API', type: 'PROXY', definitionVersion: 'V4' } as unknown as ApiDetailDto;

function renderPage(api: ApiDetailDto | null, isLoading = false) {
    return render(
        <ApiDetailContext.Provider value={{ api, isLoading, permissionsReady: true }}>
            <ApiTargetsPage />
        </ApiDetailContext.Provider>,
    );
}

describe('ApiTargetsPage', () => {
    afterEach(() => jest.clearAllMocks());

    it('mounts the shared panel on the API as its own subject, under the targets provider', () => {
        renderPage(API);

        expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Targets');
        expect(screen.getByTestId('targets-provider')).toContainElement(screen.getByTestId('targets-panel'));
        expect(mockTargetsPanel.mock.calls[0][0]).toEqual({
            reference: 'api-1',
            apiIds: ['api-1'],
            apiTypes: ['PROXY'],
            readOnly: false,
        });
    });

    it('hands the panel a read-only flag to a user who may neither create nor update environment APIs', () => {
        mockUseHasPermission.mockReturnValue(false);

        renderPage(API);

        expect(mockUseHasPermission).toHaveBeenCalledWith({ anyOf: ['environment-api-c', 'environment-api-u'] });
        expect(mockTargetsPanel.mock.calls[0][0]).toMatchObject({ readOnly: true });
    });

    it('waits for the API before mounting the panel', () => {
        renderPage(null, true);

        expect(screen.queryByTestId('targets-panel')).not.toBeInTheDocument();
    });
});
