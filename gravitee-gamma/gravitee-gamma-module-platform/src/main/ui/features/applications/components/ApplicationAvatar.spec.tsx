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

jest.mock('@gravitee/graphene-core', () => ({
    Avatar: ({ children, className }: { children?: ReactNode; className?: string }) => (
        <span data-testid="avatar" className={className}>
            {children}
        </span>
    ),
    AvatarImage: ({ src, alt }: { src: string; alt: string }) => <img src={src} alt={alt} />,
    AvatarFallback: ({ children }: { children?: ReactNode }) => <span data-testid="avatar-fallback">{children}</span>,
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

import { ApplicationAvatar } from './ApplicationAvatar';

describe('ApplicationAvatar', () => {
    it('shows the application picture when it has one', () => {
        render(<ApplicationAvatar src="https://api.example.com/applications/app-1/picture?hash=1" name="Billing App" />);

        expect(screen.getByRole('img', { name: 'Billing App logo' })).toHaveAttribute(
            'src',
            'https://api.example.com/applications/app-1/picture?hash=1',
        );
    });

    it('falls back to the generic icon, without any image to request, when it has no picture', () => {
        render(<ApplicationAvatar name="Billing App" />);

        expect(screen.queryByRole('img')).not.toBeInTheDocument();
        expect(screen.getByTestId('avatar-fallback')).toBeInTheDocument();
    });

    it('is a box with a matching outline, not the default circle', () => {
        render(<ApplicationAvatar name="Billing App" />);

        const avatar = screen.getByTestId('avatar');
        expect(avatar).toHaveClass('rounded-md');
        expect(avatar).toHaveClass('after:rounded-md');
    });
});
