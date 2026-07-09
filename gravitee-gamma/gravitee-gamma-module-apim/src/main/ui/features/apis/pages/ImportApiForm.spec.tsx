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
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

jest.mock('@gravitee/graphene-core', () => ({
    Alert: ({ children }: { children?: ReactNode }) => <div role="alert">{children}</div>,
    AlertDescription: ({ children }: { children?: ReactNode }) => <p>{children}</p>,
    Button: ({ children, onClick, disabled }: { children?: ReactNode; onClick?: () => void; disabled?: boolean }) => (
        <button type="button" onClick={onClick} disabled={disabled}>
            {children}
        </button>
    ),
    Separator: () => <hr />,
    Input: ({
        id,
        value,
        onChange,
        placeholder,
    }: {
        id?: string;
        value?: string;
        onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
        placeholder?: string;
    }) => <input id={id} value={value} onChange={onChange} placeholder={placeholder} />,
    Label: ({ children, htmlFor }: { children?: ReactNode; htmlFor?: string }) => <label htmlFor={htmlFor}>{children}</label>,
    Switch: ({ checked, onCheckedChange }: { checked?: boolean; onCheckedChange?: (v: boolean) => void }) => (
        <input type="checkbox" role="switch" checked={checked} onChange={e => onCheckedChange?.(e.target.checked)} />
    ),
    cn: (...args: string[]) => args.filter(Boolean).join(' '),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

jest.mock('../services/policyStudioService', () => ({
    listPolicies: jest.fn(() => Promise.resolve([])),
}));

jest.mock('../hooks/useCreateApiFromImport');

import { ImportApiForm } from './ImportApiForm';
import { notify } from '../../../shared/notify';
import { useCreateApiFromImport } from '../hooks/useCreateApiFromImport';

const mockUseCreateApiFromImport = useCreateApiFromImport as jest.Mock;

function fileWithText(name: string, content: string, type: string): File {
    const file = new File([content], name, { type });
    Object.defineProperty(file, 'text', { value: () => Promise.resolve(content) });
    return file;
}

// Route nesting mirrors AppRoutes.tsx exactly (apis > new > import/:format, and apis > :apiId > overview)
// since React Router v6 resolves `navigate('..')`/`navigate('../..')` by route-tree nesting level,
// not URL segment count — a flat/absolute route list here would not reproduce the same relative nav.
function renderForm(format: 'gravitee' | 'openapi' | 'wsdl' = 'gravitee') {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[`/apis/new/import/${format}`]}>
                <Routes>
                    <Route path="apis">
                        <Route path="new">
                            <Route index element={<div>Picker page</div>} />
                            <Route path="import/:format" element={<ImportApiForm format={format} />} />
                        </Route>
                        <Route path=":apiId">
                            <Route path="overview" element={<div>API overview page</div>} />
                        </Route>
                    </Route>
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('ImportApiForm', () => {
    let mutate: jest.Mock;

    beforeEach(() => {
        mutate = jest.fn();
        mockUseCreateApiFromImport.mockReturnValue({
            mutate,
            isPending: false,
            error: null,
            isSuccess: false,
            data: undefined,
        });
    });

    afterEach(() => jest.clearAllMocks());

    it('disables Create until a valid Gravitee definition file is picked, then submits it', async () => {
        const { container } = renderForm('gravitee');
        expect(screen.getByRole('button', { name: /create api/i })).toBeDisabled();

        const definition = { api: { name: 'My API' } };
        const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement;
        fireEvent.change(fileInput, { target: { files: [fileWithText('api.json', JSON.stringify(definition), 'application/json')] } });

        await waitFor(() => expect(screen.getByRole('button', { name: /create api/i })).not.toBeDisabled());
        fireEvent.click(screen.getByRole('button', { name: /create api/i }));

        expect(mutate).toHaveBeenCalledWith({ format: 'gravitee', source: 'local', definition });
    });

    it('submits an OpenAPI descriptor once a spec file is picked', async () => {
        const { container } = renderForm('openapi');
        const yaml = 'openapi: 3.0.0\ninfo:\n  title: My API';
        const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement;
        fireEvent.change(fileInput, { target: { files: [fileWithText('api.yaml', yaml, 'application/x-yaml')] } });

        await waitFor(() => expect(screen.getByRole('button', { name: /create api/i })).not.toBeDisabled());
        fireEvent.click(screen.getByRole('button', { name: /create api/i }));

        expect(mutate).toHaveBeenCalledWith({ format: 'openapi', descriptor: { payload: yaml, withDocumentation: true } });
    });

    it('shows an error alert when the mutation fails', () => {
        mockUseCreateApiFromImport.mockReturnValue({
            mutate,
            isPending: false,
            error: new Error('Failed to create the API.'),
            isSuccess: false,
            data: undefined,
        });
        renderForm('gravitee');

        expect(screen.getByRole('alert')).toHaveTextContent('Failed to create the API.');
    });

    it('navigates to the new API overview and shows a success toast once creation succeeds', () => {
        mockUseCreateApiFromImport.mockReturnValue({
            mutate,
            isPending: false,
            error: null,
            isSuccess: true,
            data: { id: 'api-123', name: 'My API' },
        });
        renderForm('gravitee');

        expect(notify.success).toHaveBeenCalledWith('API created');
        expect(screen.getByText('API overview page')).toBeInTheDocument();
    });

    it('navigates back to the picker page when Cancel is clicked', () => {
        renderForm('gravitee');

        fireEvent.click(screen.getByRole('button', { name: /cancel/i }));

        expect(screen.getByText('Picker page')).toBeInTheDocument();
    });
});
