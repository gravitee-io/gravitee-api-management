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
import userEvent from '@testing-library/user-event';

import { ViewPortalLink } from './ViewPortalLink';

describe('ViewPortalLink', () => {
    it.each(['https://portal.example.com/docs', 'http://localhost:8080'])('opens %s in an isolated new tab', url => {
        render(<ViewPortalLink url={url} portalName="Partner Portal" />);

        const link = screen.getByRole('link', { name: 'View Portal: Partner Portal' });
        expect(link).toHaveAttribute('href', url);
        expect(link).toHaveAttribute('target', '_blank');
        expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    });

    it.each([
        null,
        '',
        '/relative',
        '//portal.example.com',
        'javascript:alert(1)',
        'data:text/html,portal',
        'not-a-url',
        'https://',
        'https:/portal.example.com',
    ])('does not expose a link for an unavailable or unsafe URL: %s', url => {
        render(<ViewPortalLink url={url} portalName="Partner Portal" />);

        expect(screen.queryByRole('link')).not.toBeInTheDocument();
        const button = screen.getByRole('button', { name: 'View Portal: Partner Portal' });
        expect(button).toHaveAttribute('aria-disabled', 'true');
        expect(button).toHaveAccessibleDescription('No portal URL is configured.');
    });

    it('explains why the action is unavailable when focused', async () => {
        const user = userEvent.setup();
        render(<ViewPortalLink url={null} portalName="Partner Portal" />);

        await user.tab();

        expect(screen.getByRole('button', { name: 'View Portal: Partner Portal' })).toHaveFocus();
        expect(await screen.findByRole('tooltip')).toHaveTextContent('No portal URL is configured.');
    });
});
