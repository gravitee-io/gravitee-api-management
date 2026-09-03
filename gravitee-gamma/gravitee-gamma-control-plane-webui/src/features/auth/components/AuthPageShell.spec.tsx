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
import { render, screen } from '@testing-library/react';

import { AuthPageShell } from './AuthPageShell';

describe('AuthPageShell', () => {
    it('renders the title, the description and the content', () => {
        render(
            <AuthPageShell title="Sign in" description="to access Gravitee Gamma">
                <button type="button">Continue</button>
            </AuthPageShell>,
        );

        expect(screen.getByText('Sign in')).toBeTruthy();
        expect(screen.getByText('to access Gravitee Gamma')).toBeTruthy();
        expect(screen.getByRole('button', { name: 'Continue' })).toBeTruthy();
    });

    it('renders the footer slot when one is given', () => {
        render(
            <AuthPageShell title="Reset password" description="to access Gravitee Gamma" footer={<span>Go to sign in</span>}>
                <p>Content</p>
            </AuthPageShell>,
        );

        expect(screen.getByText('Go to sign in')).toBeTruthy();
    });

    it('renders no footer when none is given', () => {
        render(
            <AuthPageShell title="Sign in" description="to access Gravitee Gamma">
                <p>Content</p>
            </AuthPageShell>,
        );

        expect(screen.queryByText('Go to sign in')).toBeNull();
    });

    it('carries a brand mark for each theme, swapped by display', () => {
        render(
            <AuthPageShell title="Sign in" description="to access Gravitee Gamma">
                <p>Content</p>
            </AuthPageShell>,
        );

        const marks = screen.getAllByAltText('Gravitee');
        expect(marks).toHaveLength(2);

        const [lightMark, darkMark] = marks;
        // The wordmark is near-black on light and near-white on dark, so the two are distinct
        // assets rather than one recoloured mark. `.dark` on the document element picks one.
        expect(lightMark?.className).toContain('dark:hidden');
        expect(darkMark?.className).toContain('dark:block');
        expect(darkMark?.className).toContain('hidden');
        expect(lightMark?.getAttribute('src')).not.toBe(darkMark?.getAttribute('src'));
    });
});
