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
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { PolicyStudioPage } from './PolicyStudioPage';
import { usePolicyStudioData } from '../../hooks/usePolicyStudioData';
import type { PolicyStudioData } from '../../hooks/usePolicyStudioData';
import type { PolicyStudioApiDetail } from '../../types/policyStudio';

jest.mock('@gravitee/graphene-core', () => ({ useLayoutConfig: jest.fn() }));
jest.mock('@gravitee/graphene-policy-studio', () => ({ PolicyStudio: () => <div data-testid="policy-studio-editor" /> }), {
    virtual: true,
});
jest.mock('../../hooks/usePolicyStudioData');
jest.mock('../../hooks/usePolicyStudioSave', () => ({ usePolicyStudioSave: jest.fn(() => jest.fn()) }));

const mockUsePolicyStudioData = jest.mocked(usePolicyStudioData);

function baseData(overrides: Partial<PolicyStudioData> = {}): PolicyStudioData {
    return {
        apiType: 'PROXY',
        policies: [],
        sharedPolicyGroups: [],
        plans: [],
        commonFlows: [],
        entrypointsInfo: [],
        endpointsInfo: [],
        flowExecution: { mode: 'DEFAULT', matchRequired: false },
        isLoading: false,
        isError: false,
        apiDetail: undefined,
        isV2: false,
        ...overrides,
    };
}

function renderPage() {
    return render(
        <MemoryRouter initialEntries={['/apis/api-1/policy-studio']}>
            <Routes>
                <Route path="/apis/:apiId/policy-studio" element={<PolicyStudioPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

describe('PolicyStudioPage', () => {
    afterEach(() => jest.clearAllMocks());

    it('shows an error message when the studio data failed to load', () => {
        mockUsePolicyStudioData.mockReturnValue(baseData({ isError: true }));
        renderPage();
        expect(screen.getByText(/failed to load policy studio data/i)).toBeInTheDocument();
        expect(screen.queryByTestId('policy-studio-editor')).not.toBeInTheDocument();
    });

    it('shows a "not available" message instead of the editor for a TCP Proxy API', () => {
        mockUsePolicyStudioData.mockReturnValue(baseData({ apiDetail: { listeners: [{ type: 'TCP' }] } as PolicyStudioApiDetail }));
        renderPage();
        expect(screen.getByText(/policy studio is not available for tcp proxy apis/i)).toBeInTheDocument();
        expect(screen.queryByTestId('policy-studio-editor')).not.toBeInTheDocument();
    });

    it('renders the policy editor for a non-TCP API', () => {
        mockUsePolicyStudioData.mockReturnValue(baseData({ apiDetail: { listeners: [{ type: 'HTTP' }] } as PolicyStudioApiDetail }));
        renderPage();
        expect(screen.getByTestId('policy-studio-editor')).toBeInTheDocument();
    });

    it('renders the policy editor while the API detail is still loading (no false-positive TCP block)', () => {
        mockUsePolicyStudioData.mockReturnValue(baseData({ isLoading: true, apiDetail: undefined }));
        renderPage();
        expect(screen.getByTestId('policy-studio-editor')).toBeInTheDocument();
    });
});
