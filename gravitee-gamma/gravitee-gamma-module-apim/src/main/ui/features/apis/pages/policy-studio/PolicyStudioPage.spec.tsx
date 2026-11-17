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
import type { PolicyStudioProps } from '@gravitee/graphene-policy-studio';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { PolicyStudioPage } from './PolicyStudioPage';
import { usePolicyStudioData } from '../../hooks/usePolicyStudioData';
import type { PolicyStudioData } from '../../hooks/usePolicyStudioData';

const mockPolicyStudio = jest.fn((_props: PolicyStudioProps) => <div data-testid="policy-studio-editor" />);

jest.mock('@gravitee/graphene-core', () => ({ useLayoutConfig: jest.fn() }));
jest.mock('@gravitee/graphene-policy-studio', () => ({ PolicyStudio: (props: PolicyStudioProps) => mockPolicyStudio(props) }), {
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
        resources: [],
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

    it("gives the API's resources to the policy editor", () => {
        const resources = [{ name: 'My Cache Resource', type: 'cache' }];
        mockUsePolicyStudioData.mockReturnValue(baseData({ resources }));

        renderPage();

        expect(screen.getByTestId('policy-studio-editor')).toBeInTheDocument();
        expect(mockPolicyStudio).toHaveBeenCalledWith(expect.objectContaining({ resources }));
    });
});
