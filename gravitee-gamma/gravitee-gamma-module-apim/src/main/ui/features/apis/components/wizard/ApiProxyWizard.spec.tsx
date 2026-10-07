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

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({ useNavigate: () => mockNavigate }));

jest.mock('../../../../shared/notify', () => ({
    notify: { success: jest.fn(), warning: jest.fn(), error: jest.fn() },
}));

const mockUseCreateApiProxy = jest.fn();
jest.mock('../../hooks/useCreateApiProxy', () => ({
    useCreateApiProxy: () => mockUseCreateApiProxy(),
}));

jest.mock('../../hooks/useApiReviewEnabled', () => ({
    useApiReviewEnabled: () => ({ enabled: false, isFetched: true }),
}));

const mockDispatch = jest.fn();
const cleanForm = {
    apiName: '',
    apiVersion: '1.0.0',
    apiDescription: '',
    protocol: 'HTTP',
    contextPath: '/',
    virtualHostsEnabled: false,
    virtualHosts: [{ id: '1', host: '', path: '/', overrideAccess: false }],
    targetUrl: '',
    tcpHosts: [{ id: '1', host: '' }],
    tcpTargetHost: '',
    tcpTargetPort: '',
    deployImmediately: true,
    askForReview: false,
};

const mockState = {
    step: 1,
    form: { ...cleanForm, apiName: 'Flights' },
    validationErrors: {},
    isPathVerifying: false,
    creationMode: 'template',
};
jest.mock('../../store/apiCreationStore', () => ({
    useApiCreation: () => ({ state: mockState, dispatch: mockDispatch }),
}));

jest.mock('../steps/EssentialsStep', () => ({ EssentialsStep: () => <div /> }));
jest.mock('../steps/ReviewDeployStep', () => ({ ReviewDeployStep: () => <div /> }));
jest.mock('./ProxyFlowVisualization', () => ({ ProxyFlowVisualization: () => <div /> }));
jest.mock('./StepProgress', () => ({ StepProgress: () => <div /> }));

import { ApiProxyWizard } from './ApiProxyWizard';
import { ApimApiError } from '../../../../shared/api/apimClient';
import { notify } from '../../../../shared/notify';

const mockReset = jest.fn();

type MutateOptions = { onSuccess?: (data: unknown) => void };

function mutationState(overrides: Record<string, unknown> = {}) {
    const { onSuccessData, ...rest } = overrides as { onSuccessData?: unknown; [k: string]: unknown };
    return {
        mutate: jest.fn((_vars: unknown, opts?: MutateOptions) => {
            if (onSuccessData) opts?.onSuccess?.(onSuccessData);
        }),
        reset: mockReset,
        isPending: false,
        error: null,
        ...rest,
    };
}

function renderWizard() {
    render(<ApiProxyWizard mode="template" />);
}

describe('ApiProxyWizard — after the creation call', () => {
    beforeEach(() => jest.clearAllMocks());

    it('navigates to the created API even when a step after creation failed', () => {
        mockUseCreateApiProxy.mockReturnValue(
            mutationState({
                onSuccessData: {
                    api: { id: 'api-1', name: 'Flights' },
                    warnings: ['The plan could not be created. Open the API to add a plan. (Plan name already used.)'],
                },
            }),
        );

        renderWizard();
        fireEvent.click(screen.getByRole('button', { name: /create & deploy/i }));

        expect(mockNavigate).toHaveBeenCalledWith('../../api-1/overview');
    });

    it('raises one warning toast per failed step', () => {
        const warnings = ['The plan could not be created. Open the API to add a plan. (Nope.)', 'The review could not be requested.'];
        mockUseCreateApiProxy.mockReturnValue(mutationState({ onSuccessData: { api: { id: 'api-1', name: 'Flights' }, warnings } }));

        renderWizard();
        fireEvent.click(screen.getByRole('button', { name: /create & deploy/i }));

        expect(notify.warning).toHaveBeenCalledTimes(2);
        expect(notify.warning).toHaveBeenCalledWith(warnings[0]);
        expect(notify.warning).toHaveBeenCalledWith(warnings[1]);
        expect(notify.error).not.toHaveBeenCalled();
    });

    it('shows only the success toast when nothing failed', () => {
        mockUseCreateApiProxy.mockReturnValue(mutationState({ onSuccessData: { api: { id: 'api-1', name: 'Flights' }, warnings: [] } }));

        renderWizard();
        fireEvent.click(screen.getByRole('button', { name: /create & deploy/i }));

        expect(notify.success).toHaveBeenCalledWith('API created');
        expect(notify.warning).not.toHaveBeenCalled();
        expect(mockNavigate).toHaveBeenCalledWith('../../api-1/overview');
    });

    it('keeps the user on the wizard and shows the alert when the API could not be created', () => {
        mockUseCreateApiProxy.mockReturnValue(mutationState({ error: new ApimApiError(400, 'Context path already used.') }));

        renderWizard();

        expect(screen.getByText('Context path already used.')).toBeInTheDocument();
        expect(mockNavigate).not.toHaveBeenCalled();
    });

    it('clears the creation error when the user steps back to fix the form', () => {
        mockUseCreateApiProxy.mockReturnValue(mutationState({ error: new ApimApiError(400, 'Context path already used.') }));

        renderWizard();
        fireEvent.click(screen.getByRole('button', { name: /back/i }));

        expect(mockReset).toHaveBeenCalled();
    });
});

describe('ApiProxyWizard — Cancel', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseCreateApiProxy.mockReturnValue(mutationState());
        mockState.step = 0;
        mockState.form = { ...cleanForm };
    });

    afterEach(() => {
        mockState.step = 1;
        mockState.form = { ...cleanForm, apiName: 'Flights' };
    });

    it('leaves immediately when the form is untouched', () => {
        renderWizard();
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

        expect(screen.queryByRole('button', { name: 'Discard changes' })).not.toBeInTheDocument();
        expect(mockNavigate).toHaveBeenCalledWith('..');
    });

    it('asks before leaving a dirty form, and Keep creating stays on the page', () => {
        mockState.form = { ...cleanForm, apiName: 'Orders' };
        renderWizard();
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

        expect(screen.getByRole('button', { name: 'Discard changes' })).toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', { name: 'Keep creating' }));
        expect(mockNavigate).not.toHaveBeenCalled();
    });

    it('navigates away when Discard changes is confirmed', () => {
        mockState.form = { ...cleanForm, apiName: 'Orders' };
        renderWizard();
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        fireEvent.click(screen.getByRole('button', { name: 'Discard changes' }));

        expect(mockNavigate).toHaveBeenCalledWith('..');
    });
});
