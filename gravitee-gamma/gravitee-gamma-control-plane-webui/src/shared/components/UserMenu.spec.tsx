/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { ThemeProvider, useTheme } from '@gravitee/graphene-core';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { UserMenu } from './UserMenu';

function ThemeProbe() {
    const { mode } = useTheme();
    return <span data-testid="theme-mode">{mode}</span>;
}

function renderMenu(props: Partial<Parameters<typeof UserMenu>[0]> = {}) {
    return render(
        <ThemeProvider defaultMode="system">
            <ThemeProbe />
            <UserMenu
                name="Ada Lovelace"
                email="ada@example.com"
                avatarSrc="http://api/avatar?1"
                onMyAccount={jest.fn()}
                onSignOut={jest.fn()}
                {...props}
            />
        </ThemeProvider>,
    );
}

describe('UserMenu', () => {
    beforeAll(() => {
        Element.prototype.hasPointerCapture = jest.fn();
        Element.prototype.setPointerCapture = jest.fn();
        Element.prototype.releasePointerCapture = jest.fn();
    });

    beforeEach(() => {
        localStorage.clear();
    });

    it('should open My Account and Sign out from the avatar menu', async () => {
        const user = userEvent.setup();
        const onMyAccount = jest.fn();
        const onSignOut = jest.fn();

        renderMenu({ onMyAccount, onSignOut });

        await user.click(screen.getByRole('button', { name: 'Account menu' }));
        expect(screen.getByText('Ada Lovelace')).toBeTruthy();
        expect(screen.getByText('ada@example.com')).toBeTruthy();

        await user.click(screen.getByRole('menuitem', { name: 'My Account' }));
        expect(onMyAccount).toHaveBeenCalledTimes(1);

        await user.click(screen.getByRole('button', { name: 'Account menu' }));
        await user.click(screen.getByRole('menuitem', { name: 'Sign out' }));
        expect(onSignOut).toHaveBeenCalledTimes(1);
    });

    it('should offer the Light, Dark, and System theme options that Graphene TopNavUser used to own', async () => {
        const user = userEvent.setup();
        renderMenu();

        await user.click(screen.getByRole('button', { name: 'Account menu' }));

        expect(screen.getByRole('menuitemradio', { name: 'Light' })).toBeTruthy();
        expect(screen.getByRole('menuitemradio', { name: 'Dark' })).toBeTruthy();
        expect(screen.getByRole('menuitemradio', { name: 'System' }).getAttribute('aria-checked')).toBe('true');
    });

    it('should switch the theme when another mode is selected', async () => {
        const user = userEvent.setup();
        renderMenu();

        await user.click(screen.getByRole('button', { name: 'Account menu' }));
        await user.click(screen.getByRole('menuitemradio', { name: 'Dark' }));

        expect(screen.getByTestId('theme-mode').textContent).toBe('dark');

        await user.click(screen.getByRole('button', { name: 'Account menu' }));
        expect(screen.getByRole('menuitemradio', { name: 'Dark' }).getAttribute('aria-checked')).toBe('true');
    });
});
