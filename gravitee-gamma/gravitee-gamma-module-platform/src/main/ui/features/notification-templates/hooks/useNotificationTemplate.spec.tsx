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
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useNotificationTemplate } from './useNotificationTemplate';
import { searchNotificationTemplates } from '../services/notificationTemplates';
import type { NotificationTemplate } from '../types/notificationTemplate';

jest.mock('../services/notificationTemplates', () => ({
    searchNotificationTemplates: jest.fn(),
    persistNotificationTemplate: jest.fn(),
}));

const mockSearch = jest.mocked(searchNotificationTemplates);

const HEADER: NotificationTemplate = {
    name: 'header.html',
    scope: 'TEMPLATES_TO_INCLUDE',
    type: 'EMAIL',
    hook: '',
    content: '<header></header>',
};

const FOOTER: NotificationTemplate = {
    ...HEADER,
    name: 'footer.html',
    content: '<footer></footer>',
};

function wrapper({ children }: { children: ReactNode }) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe('useNotificationTemplate', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('keeps only matching names from a multi-row hook= include-fragment response', async () => {
        mockSearch.mockResolvedValue([HEADER, FOOTER]);

        const { result } = renderHook(() => useNotificationTemplate('TEMPLATES_TO_INCLUDE', 'header.html'), { wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(mockSearch).toHaveBeenCalledWith({ scope: 'TEMPLATES_TO_INCLUDE', hook: '' });
        expect(result.current.data).toEqual([HEADER]);
    });

    it('returns every channel for a named hook', async () => {
        const email: NotificationTemplate = { name: 'API Started', scope: 'API', type: 'EMAIL', hook: 'API_STARTED', content: '<p>x</p>' };
        const portal: NotificationTemplate = { ...email, type: 'PORTAL', content: 'x' };
        mockSearch.mockResolvedValue([email, portal]);

        const { result } = renderHook(() => useNotificationTemplate('API', 'API_STARTED'), { wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(mockSearch).toHaveBeenCalledWith({ scope: 'API', hook: 'API_STARTED' });
        expect(result.current.data).toEqual([email, portal]);
    });
});
