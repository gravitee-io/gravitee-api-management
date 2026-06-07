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
import { useParsedSchema } from '../useParsedSchema';

const getParsedSchemaSpy = vi.fn();

vi.mock('../../api/authz-api.service', () => ({
    DEFAULT_PER_PAGE: 10,
    authzApiService: {
        getParsedSchema: (env: string) => getParsedSchemaSpy(env),
    },
}));

function makeWrapper() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

function Probe({ env }: { env: string }) {
    const { parsed } = useParsedSchema(env);
    return (
        <div>
            <span data-testid="ents">{parsed.entities.map(e => e.name).join(',') || 'none'}</span>
            <span data-testid="acts">
                {parsed.actions.map(a => `${a.name}:${a.principals.join('+')}/${a.resources.join('+')}`).join(',') || 'none'}
            </span>
        </div>
    );
}

beforeEach(() => getParsedSchemaSpy.mockReset());

describe('useParsedSchema', () => {
    it('does not fire the query for an empty environmentId', async () => {
        getParsedSchemaSpy.mockResolvedValue({});
        render(<Probe env="" />, { wrapper: makeWrapper() });
        await Promise.resolve();
        await Promise.resolve();
        expect(getParsedSchemaSpy).not.toHaveBeenCalled();
    });

    it('maps the engine JSON, qualifying local types and keeping global refs bare', async () => {
        // Real engine shape: Subject is global (under ""), Report is in myapp; the myapp action
        // references Subject bare → must resolve to the global Subject, not myapp::Subject.
        getParsedSchemaSpy.mockResolvedValue({
            '': { entityTypes: { Subject: {} }, actions: {} },
            myapp: {
                entityTypes: { Report: {} },
                actions: { read: { appliesTo: { principalTypes: ['Subject'], resourceTypes: ['Report'] } } },
            },
        });

        const { getByTestId } = render(<Probe env="env-1" />, { wrapper: makeWrapper() });

        await waitFor(() => expect(getByTestId('ents').textContent).toBe('Subject,myapp::Report'));
        expect(getByTestId('acts').textContent).toBe('read:Subject/myapp::Report');
    });

    it('yields empty collections on the first render, before data resolves', () => {
        getParsedSchemaSpy.mockResolvedValue({ myapp: { entityTypes: { Report: {} } } });
        // Assert synchronously, before the query promise settles on a later microtask.
        const { getByTestId } = render(<Probe env="env-1" />, { wrapper: makeWrapper() });
        expect(getByTestId('ents').textContent).toBe('none');
        expect(getByTestId('acts').textContent).toBe('none');
    });
});
