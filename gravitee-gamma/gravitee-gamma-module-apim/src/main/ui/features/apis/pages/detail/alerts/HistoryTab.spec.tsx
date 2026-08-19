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
import { render, screen } from '@testing-library/react';
import type { ReactNode } from 'react';

import { HistoryTab } from './HistoryTab';
import { formatAbsoluteDateTime } from '../../../../../shared/time';

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('@gravitee/graphene-core', () => {
    const actual = jest.requireActual('@gravitee/graphene-core');
    return {
        ...actual,
        Tooltip: ({ children }: { children: ReactNode }) => children,
        TooltipProvider: ({ children }: { children: ReactNode }) => children,
        TooltipTrigger: ({ children }: { children: ReactNode }) => children,
        TooltipContent: ({ children }: { children: ReactNode }) => <span>{children}</span>,
    };
});

describe('HistoryTab', () => {
    it('renders relative time from created_at epoch millis, not Invalid Date', () => {
        const createdAt = Date.now() - 3 * 60_000;
        render(
            <HistoryTab
                historyPage={{
                    content: [{ message: '[response.response_time: 304] is greater than [100.0]', created_at: createdAt }],
                    totalElements: 1,
                }}
            />,
        );

        expect(screen.queryByText('Invalid Date')).toBeNull();
        expect(screen.getByText(/minutes ago/)).not.toBeNull();
        expect(screen.getByText(/response.response_time: 304/)).not.toBeNull();
    });

    it('shows an absolute timestamp in a tooltip', () => {
        const createdAt = Date.now() - 3 * 60_000;
        render(
            <HistoryTab
                historyPage={{
                    content: [{ message: 'Health check failed', created_at: createdAt }],
                    totalElements: 1,
                }}
            />,
        );

        expect(screen.getByText(/minutes ago/)).not.toBeNull();
        expect(screen.getByText(formatAbsoluteDateTime(createdAt))).not.toBeNull();
    });

    it('renders two events that share a message', () => {
        const error = jest.spyOn(console, 'error').mockImplementation(() => undefined);
        const message = 'Health check failed';
        render(
            <HistoryTab
                historyPage={{
                    content: [
                        { message, created_at: Date.now() - 60_000 },
                        { message, created_at: Date.now() - 120_000 },
                    ],
                    totalElements: 2,
                }}
            />,
        );

        expect(screen.getAllByText(message)).toHaveLength(2);
        expect(error.mock.calls.flat().join(' ')).not.toMatch(/same key/);
        error.mockRestore();
    });
});
