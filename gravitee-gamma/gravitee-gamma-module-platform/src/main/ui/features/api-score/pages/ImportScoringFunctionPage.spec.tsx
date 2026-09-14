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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ImportScoringFunctionPage } from './ImportScoringFunctionPage';
import { notify } from '../../../shared/notify';
import { useCreateScoringFunction, useScoringFunctions } from '../hooks/useScoringFunctions';
import type { ScoringFunction } from '../types/rulesets';

jest.mock('../hooks/useScoringFunctions', () => ({
    useScoringFunctions: jest.fn(),
    useCreateScoringFunction: jest.fn(),
}));

jest.mock('../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn(), info: jest.fn() },
}));

const mockUseScoringFunctions = jest.mocked(useScoringFunctions);
const mockUseCreateScoringFunction = jest.mocked(useCreateScoringFunction);
const mockNotify = jest.mocked(notify);

const FN: ScoringFunction = {
    name: 'checkTag.js',
    payload: 'module.exports = { old: true }',
    createdAt: '2026-01-01T00:00:00Z',
    referenceId: 'DEFAULT',
    referenceType: 'ENVIRONMENT',
};

function mutationMock(mutateAsync = jest.fn().mockResolvedValue(undefined)) {
    return { mutateAsync, isPending: false };
}

function functionsResult(overrides: Partial<ReturnType<typeof useScoringFunctions>> = {}): ReturnType<typeof useScoringFunctions> {
    return {
        functions: [],
        isLoading: false,
        isError: false,
        error: null,
        ...overrides,
    };
}

function renderPage() {
    return render(
        <MemoryRouter initialEntries={['/api-score/rulesets/import-function']}>
            <Routes>
                <Route path="/api-score/rulesets/import-function" element={<ImportScoringFunctionPage />} />
                <Route path="/api-score/rulesets" element={<div data-testid="rulesets-list" />} />
            </Routes>
        </MemoryRouter>,
    );
}

async function uploadJs(user: ReturnType<typeof userEvent.setup>, name: string, content = 'module.exports = {}') {
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    await user.upload(input, new File([content], name, { type: 'text/javascript' }));
}

describe('ImportScoringFunctionPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseScoringFunctions.mockReturnValue(functionsResult());
        mockUseCreateScoringFunction.mockReturnValue(mutationMock() as ReturnType<typeof useCreateScoringFunction>);
    });

    it('imports a new function and navigates back', async () => {
        const user = userEvent.setup();
        const mutateAsync = jest.fn().mockResolvedValue(undefined);
        mockUseCreateScoringFunction.mockReturnValue(mutationMock(mutateAsync) as ReturnType<typeof useCreateScoringFunction>);
        renderPage();

        expect(screen.getByTestId('import-button')).toHaveProperty('disabled', true);
        await uploadJs(user, 'checkTag.js');
        await waitFor(() => expect(screen.getByTestId('import-button')).toHaveProperty('disabled', false));
        await user.click(screen.getByTestId('import-button'));

        await waitFor(() => expect(mutateAsync).toHaveBeenCalledWith({ name: 'checkTag.js', payload: 'module.exports = {}' }));
        expect(mockNotify.success).toHaveBeenCalledWith('Function imported.');
        expect(screen.getByTestId('rulesets-list')).not.toBeNull();
    });

    it('asks to overwrite when the filename already exists', async () => {
        const user = userEvent.setup();
        const mutateAsync = jest.fn().mockResolvedValue(undefined);
        mockUseScoringFunctions.mockReturnValue(functionsResult({ functions: [FN] }));
        mockUseCreateScoringFunction.mockReturnValue(mutationMock(mutateAsync) as ReturnType<typeof useCreateScoringFunction>);
        renderPage();

        await uploadJs(user, 'checkTag.js', 'module.exports = { next: true }');
        await waitFor(() => expect(screen.getByTestId('import-button')).toHaveProperty('disabled', false));
        await user.click(screen.getByTestId('import-button'));

        expect(mutateAsync).not.toHaveBeenCalled();
        expect(screen.getByText(/A function with the same name already exists/)).not.toBeNull();
        await user.click(screen.getByRole('button', { name: 'Overwrite' }));

        await waitFor(() => expect(mutateAsync).toHaveBeenCalledWith({ name: 'checkTag.js', payload: 'module.exports = { next: true }' }));
        expect(mockNotify.success).toHaveBeenCalledWith('Function overwritten successfully!');
    });

    it('toasts Console filename errors', async () => {
        const user = userEvent.setup();
        renderPage();

        await uploadJs(user, 'path/checkTag.js');
        await waitFor(() =>
            expect(mockNotify.error).toHaveBeenCalledWith(
                expect.objectContaining({ message: 'File name should fulfill ^[^/]+\\.js$ pattern' }),
            ),
        );
        expect(screen.getByTestId('import-button')).toHaveProperty('disabled', true);
    });
});
