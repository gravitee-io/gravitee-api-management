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

import { TooltipProvider } from '@gravitee/graphene-core';
import { renderWithGraphene } from '@gravitee/graphene-core/testing';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactElement } from 'react';

import { TruncatedDisplayText } from './TruncatedDisplayText';

function renderTruncatedDisplayText(ui: ReactElement) {
    return renderWithGraphene(<TooltipProvider delayDuration={300}>{ui}</TooltipProvider>);
}

describe('TruncatedDisplayText', () => {
    beforeAll(() => {
        Object.defineProperty(window, 'matchMedia', {
            writable: true,
            value: jest.fn().mockImplementation((query: string) => ({
                matches: false,
                media: query,
                onchange: null,
                addListener: jest.fn(),
                removeListener: jest.fn(),
                addEventListener: jest.fn(),
                removeEventListener: jest.fn(),
                dispatchEvent: jest.fn(),
            })),
        });
        global.ResizeObserver = class ResizeObserver {
            observe() {}
            unobserve() {}
            disconnect() {}
        } as typeof ResizeObserver;
    });

    it('renders the display text without a tooltip when the list is not truncated', () => {
        renderTruncatedDisplayText(
            <TruncatedDisplayText displayText="ADMIN, USER" isPlaceholder={false} showTooltip={false} labels={['ADMIN', 'USER']} />,
        );
        expect(screen.getByText('ADMIN, USER')).not.toBeNull();
        expect(screen.queryByRole('tooltip')).toBeNull();
    });

    it('shows every label in a tooltip when the display text is truncated', async () => {
        const user = userEvent.setup();
        renderTruncatedDisplayText(
            <TruncatedDisplayText
                displayText="ADMIN, USER, ORG..."
                isPlaceholder={false}
                showTooltip
                labels={['ADMIN', 'USER', 'ORG', 'API_PUBLISHER']}
            />,
        );
        await user.hover(screen.getByText('ADMIN, USER, ORG...'));
        expect((await screen.findByRole('tooltip')).textContent).toBe('ADMIN, USER, ORG, API_PUBLISHER');
    });
});
