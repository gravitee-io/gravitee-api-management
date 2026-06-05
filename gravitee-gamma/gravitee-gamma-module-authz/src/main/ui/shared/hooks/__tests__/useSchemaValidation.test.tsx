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
import { render, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useSchemaValidation } from '../useSchemaValidation';

const validateSchemaSpy = vi.fn();

vi.mock('../../api/authz-api.service', () => ({
    DEFAULT_PER_PAGE: 10,
    authzApiService: {
        validateSchema: (env: string, text: string) => validateSchemaSpy(env, text),
    },
}));

function makeWrapper() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

function Probe({ env, text, enabled }: { env: string; text: string; enabled: boolean }) {
    const { errors } = useSchemaValidation(env, text, enabled);
    return <span data-testid="errors">{errors.join('|') || 'none'}</span>;
}

beforeEach(() => validateSchemaSpy.mockReset());

describe('useSchemaValidation', () => {
    it('returns no errors and never calls the backend when disabled', async () => {
        validateSchemaSpy.mockResolvedValue({ valid: false, errors: ['nope'] });
        const { getByTestId } = render(<Probe env="env-1" text="entity X {};" enabled={false} />, { wrapper: makeWrapper() });
        await Promise.resolve();
        await Promise.resolve();
        expect(getByTestId('errors').textContent).toBe('none');
        expect(validateSchemaSpy).not.toHaveBeenCalled();
    });

    it('flags an empty draft locally without hitting the backend', async () => {
        const { getByTestId } = render(<Probe env="env-1" text="   " enabled={true} />, { wrapper: makeWrapper() });
        await waitFor(() => expect(getByTestId('errors').textContent).toBe('Schema must not be empty.'));
        expect(validateSchemaSpy).not.toHaveBeenCalled();
    });

    it('surfaces backend parse errors for an invalid draft', async () => {
        validateSchemaSpy.mockResolvedValue({ valid: false, errors: ['line 1:0 token recognition error'] });
        const { getByTestId } = render(<Probe env="env-1" text="@@@" enabled={true} />, { wrapper: makeWrapper() });
        await waitFor(() => expect(getByTestId('errors').textContent).toBe('line 1:0 token recognition error'));
    });

    it('reports no errors for a valid draft', async () => {
        validateSchemaSpy.mockResolvedValue({ valid: true, errors: [] });
        const { getByTestId } = render(<Probe env="env-1" text="entity User {};" enabled={true} />, { wrapper: makeWrapper() });
        await waitFor(() => expect(validateSchemaSpy).toHaveBeenCalledWith('env-1', 'entity User {};'));
        expect(getByTestId('errors').textContent).toBe('none');
    });
});
