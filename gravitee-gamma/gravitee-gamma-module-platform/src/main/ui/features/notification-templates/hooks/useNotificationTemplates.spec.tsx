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

import { useNotificationTemplates } from './useNotificationTemplates';
import { useConsoleSettings } from '../../../shared/console-settings';
import { listNotificationTemplates } from '../services/notificationTemplates';
import type { NotificationTemplate } from '../types/notificationTemplate';

jest.mock('../services/notificationTemplates', () => ({
    listNotificationTemplates: jest.fn(),
}));

jest.mock('../../../shared/console-settings', () => ({
    useConsoleSettings: jest.fn(),
}));

const mockList = jest.mocked(listNotificationTemplates);
const mockUseConsoleSettings = jest.mocked(useConsoleSettings);

const API_STARTED: NotificationTemplate = {
    name: 'API Started',
    scope: 'API',
    type: 'EMAIL',
    hook: 'API_STARTED',
    content: '<p>x</p>',
};

const ALERT: NotificationTemplate = {
    name: 'HTTP status code',
    scope: 'TEMPLATES_FOR_ALERT',
    type: 'EMAIL',
    hook: 'CONSUMER_HTTP_STATUS',
    content: '<p>alert</p>',
};

function wrapper({ children }: { children: ReactNode }) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe('useNotificationTemplates', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockList.mockResolvedValue([API_STARTED, ALERT]);
    });

    it('shows Templates for alert from bootstrap console settings, not GET /settings', async () => {
        mockUseConsoleSettings.mockReturnValue({ alert: { enabled: true } });

        const { result } = renderHook(() => useNotificationTemplates(), { wrapper });

        await waitFor(() => expect(result.current.isLoading).toBe(false));
        expect(result.current.categories.map(category => category.scope)).toEqual(['API', 'TEMPLATES_FOR_ALERT']);
        expect(result.current.templateCount).toBe(2);
    });

    it('hides Templates for alert when the bootstrap flag is off', async () => {
        mockUseConsoleSettings.mockReturnValue({ alert: { enabled: false } });

        const { result } = renderHook(() => useNotificationTemplates(), { wrapper });

        await waitFor(() => expect(result.current.isLoading).toBe(false));
        expect(result.current.categories.map(category => category.scope)).toEqual(['API']);
        expect(result.current.templateCount).toBe(1);
    });
});
