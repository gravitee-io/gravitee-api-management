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
            <AuthPageShell title="Sign in" description="Choose a new password for your account.">
                <button type="button">Continue</button>
            </AuthPageShell>,
        );

        expect(screen.getByText('Sign in')).toBeTruthy();
        expect(screen.getByText('Choose a new password for your account.')).toBeTruthy();
        expect(screen.getByRole('button', { name: 'Continue' })).toBeTruthy();
    });

    it('renders no description when none is given', () => {
        // Asserting a particular string is absent would pass against a shell that always
        // rendered an empty CardDescription, which is the case the conditional exists for.
        const { container } = render(
            <AuthPageShell title="Sign in">
                <p>Content</p>
            </AuthPageShell>,
        );

        expect(screen.getByText('Sign in')).toBeTruthy();
        expect(container.querySelector('[data-slot="card-description"]')).toBeNull();
    });

    it('renders the footer slot when one is given', () => {
        render(
            <AuthPageShell title="Reset password" description="Choose a new password for your account." footer={<span>Go to sign in</span>}>
                <p>Content</p>
            </AuthPageShell>,
        );

        expect(screen.getByText('Go to sign in')).toBeTruthy();
    });

    it('renders no footer when none is given', () => {
        const { container } = render(
            <AuthPageShell title="Sign in" description="Choose a new password for your account.">
                <p>Content</p>
            </AuthPageShell>,
        );

        expect(container.querySelector('[data-slot="auth-page-footer"]')).toBeNull();
    });

    it('carries one brand mark, serving both themes', () => {
        render(
            <AuthPageShell title="Sign in" description="Choose a new password for your account.">
                <p>Content</p>
            </AuthPageShell>,
        );

        // The G carries its own colour and reads on either surface, so there is no theme swap
        // and no second asset to keep in step.
        const marks = screen.getAllByAltText('Gravitee');
        expect(marks).toHaveLength(1);
        expect(marks[0]?.className).not.toContain('dark:');
    });
});
