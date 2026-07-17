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
import { readFileSync } from 'node:fs';
import { join } from 'node:path';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { MobileNavDrawer } from './MobileNavDrawer';
import styles from './MobileNavDrawer.module.scss';

const drawerStyles = readFileSync(join(__dirname, 'MobileNavDrawer.module.scss'), 'utf8');

describe('MobileNavDrawer', () => {
    it('should position the backdrop absolutely within positioned ancestors', () => {
        expect(drawerStyles).toMatch(/\.backdrop\s*\{[^}]*position:\s*absolute/s);
        expect(drawerStyles).not.toMatch(/position:\s*fixed/);
        expect(drawerStyles).toContain('width: min(85%, 320px)');
    });

    it('should not render when closed', () => {
        render(
            <div style={{ position: 'relative', width: 390, height: 600 }}>
                <MobileNavDrawer open={false} onClose={jest.fn()}>
                    <div>Nav items</div>
                </MobileNavDrawer>
            </div>,
        );

        expect(screen.queryByRole('navigation', { name: 'Navigation' })).not.toBeInTheDocument();
    });

    it('should render backdrop and panel with containment classes when open', () => {
        render(
            <div data-testid="container" style={{ position: 'relative', width: 390, height: 600 }}>
                <MobileNavDrawer open onClose={jest.fn()}>
                    <div>Nav items</div>
                </MobileNavDrawer>
            </div>,
        );

        const backdrop = screen.getByRole('navigation', { name: 'Navigation' }).parentElement;
        expect(backdrop).toHaveClass(styles.backdrop);

        const panel = screen.getByRole('navigation', { name: 'Navigation' });
        expect(panel).toHaveClass(styles.panel);
        expect(screen.getByText('Nav items')).toBeInTheDocument();
    });

    it('should call onClose when backdrop is clicked', async () => {
        const user = userEvent.setup();
        const onClose = jest.fn();

        render(
            <div style={{ position: 'relative', width: 390, height: 600 }}>
                <MobileNavDrawer open onClose={onClose}>
                    <div>Nav items</div>
                </MobileNavDrawer>
            </div>,
        );

        const backdrop = screen.getByRole('navigation', { name: 'Navigation' }).parentElement!;
        await user.click(backdrop);

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('should call onClose when close button is clicked', async () => {
        const user = userEvent.setup();
        const onClose = jest.fn();

        render(
            <div style={{ position: 'relative', width: 390, height: 600 }}>
                <MobileNavDrawer open onClose={onClose}>
                    <div>Nav items</div>
                </MobileNavDrawer>
            </div>,
        );

        await user.click(screen.getByRole('button', { name: 'Close menu' }));

        expect(onClose).toHaveBeenCalledTimes(1);
    });
});
