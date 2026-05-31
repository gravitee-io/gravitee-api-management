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
import { MemoryRouter } from 'react-router-dom';

import { RegisterApplicationForm } from './RegisterApplicationForm';
import { notify } from '../../../../shared/notify';
import { APPLICATION_TYPES_FIXTURE } from '../../fixtures/applicationTypes.fixture';
import { useApplicationGroups } from '../../hooks/useApplicationGroups';
import { useApplicationTypes } from '../../hooks/useApplicationTypes';
import { useCreateApplication } from '../../hooks/useCreateApplication';
import { useUserGroupRequired } from '../../hooks/useUserGroupRequired';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}));

jest.mock('../../../../shared/notify', () => ({
    notify: {
        success: jest.fn(),
        error: jest.fn(),
        warning: jest.fn(),
        info: jest.fn(),
    },
}));

jest.mock('../../hooks/useApplicationTypes');
jest.mock('../../hooks/useApplicationGroups');
jest.mock('../../hooks/useUserGroupRequired');
jest.mock('../../hooks/useCreateApplication');

const mockUseApplicationTypes = jest.mocked(useApplicationTypes);
const mockUseApplicationGroups = jest.mocked(useApplicationGroups);
const mockUseUserGroupRequired = jest.mocked(useUserGroupRequired);
const mockUseCreateApplication = jest.mocked(useCreateApplication);
const mockNotify = jest.mocked(notify);

function renderForm() {
    return render(
        <MemoryRouter initialEntries={['/applications/new']}>
            <RegisterApplicationForm />
        </MemoryRouter>,
    );
}

describe('RegisterApplicationForm', () => {
    let mutate: jest.Mock;

    beforeEach(() => {
        mutate = jest.fn();
        mockNavigate.mockClear();
        mockUseApplicationTypes.mockReturnValue({
            data: APPLICATION_TYPES_FIXTURE,
            isLoading: false,
        } as ReturnType<typeof useApplicationTypes>);
        mockUseApplicationGroups.mockReturnValue({
            data: [],
            isLoading: false,
        } as ReturnType<typeof useApplicationGroups>);
        mockUseUserGroupRequired.mockReturnValue({ requireUserGroups: false });
        mockUseCreateApplication.mockReturnValue({
            mutate,
            isPending: false,
        } as ReturnType<typeof useCreateApplication>);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('shows a success toast and navigates to general after create succeeds', () => {
        renderForm();

        fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'Billing App' } });
        fireEvent.change(screen.getByLabelText(/Description/i), { target: { value: 'Billing integration' } });
        fireEvent.click(screen.getByRole('button', { name: /Create Application/i }));

        expect(mutate).toHaveBeenCalledTimes(1);

        const mutationOptions = mutate.mock.calls[0]?.[1] as { onSuccess?: (created: { id: string }) => void };
        mutationOptions.onSuccess?.({ id: 'app-new' });

        expect(mockNotify.success).toHaveBeenCalledWith('Application created');
        expect(mockNavigate).toHaveBeenCalledWith('../app-new/general');
    });

    it('shows an error toast when create fails', () => {
        renderForm();

        fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'Billing App' } });
        fireEvent.change(screen.getByLabelText(/Description/i), { target: { value: 'Billing integration' } });
        fireEvent.click(screen.getByRole('button', { name: /Create Application/i }));

        const mutationOptions = mutate.mock.calls[0]?.[1] as { onError?: (error: Error) => void };
        mutationOptions.onError?.(new Error('Create failed'));

        expect(mockNotify.error).toHaveBeenCalledWith(expect.any(Error), 'An error occurred while creating the application!');
        expect(mockNotify.success).not.toHaveBeenCalled();
    });
});
