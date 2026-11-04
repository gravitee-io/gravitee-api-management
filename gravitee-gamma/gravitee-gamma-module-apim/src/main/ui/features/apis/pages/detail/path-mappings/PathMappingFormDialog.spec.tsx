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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactElement } from 'react';

import { PathMappingFormDialog } from './PathMappingFormDialog';
import { listApiSwaggerPages } from '../../../services/apis';

jest.mock('../../../services/apis', () => ({
    listApiSwaggerPages: jest.fn(),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

// jsdom ships no ResizeObserver; RadioGroup / Checkbox mount one.
globalThis.ResizeObserver ??= class {
    observe() {}
    unobserve() {}
    disconnect() {}
};

const mockListSwagger = listApiSwaggerPages as jest.Mock;

function renderDialog(ui: ReactElement) {
    return render(
        <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>{ui}</QueryClientProvider>,
    );
}

beforeEach(() => {
    jest.clearAllMocks();
    mockListSwagger.mockResolvedValue([{ id: 'sw-1', name: 'Petstore' }]);
});

it('imports from a selected Swagger page with overwrite', async () => {
    const user = userEvent.setup();
    const onImport = jest.fn();
    renderDialog(
        <PathMappingFormDialog
            open
            mode="add"
            existing={[]}
            environmentId="DEFAULT"
            apiId="api-1"
            onOpenChange={jest.fn()}
            onSubmit={jest.fn()}
            onImport={onImport}
        />,
    );

    await user.click(screen.getByRole('tab', { name: 'Swagger Document' }));
    await waitFor(() => expect(screen.getByText('Petstore')).toBeInTheDocument());
    await user.click(screen.getByText('Petstore'));
    await user.click(screen.getByTestId('replace-existing-path-mappings'));
    await user.click(screen.getByTestId('path-mapping-submit'));

    expect(onImport).toHaveBeenCalledWith('sw-1', true);
});

it('shows empty swagger status when the API has no SWAGGER pages', async () => {
    const user = userEvent.setup();
    mockListSwagger.mockResolvedValue([]);
    renderDialog(
        <PathMappingFormDialog
            open
            mode="add"
            existing={[]}
            environmentId="DEFAULT"
            apiId="api-1"
            onOpenChange={jest.fn()}
            onSubmit={jest.fn()}
            onImport={jest.fn()}
        />,
    );

    await user.click(screen.getByRole('tab', { name: 'Swagger Document' }));
    await waitFor(() => expect(screen.getByTestId('swagger-pages-status')).toHaveTextContent(/No swagger documentation/i));
    expect(screen.getByTestId('path-mapping-submit')).toBeDisabled();
});
