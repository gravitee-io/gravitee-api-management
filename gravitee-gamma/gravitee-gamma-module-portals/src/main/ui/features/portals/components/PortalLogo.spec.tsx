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
import { act, render, screen } from '@testing-library/react';

import { PortalLogo } from './PortalLogo';

describe('PortalLogo', () => {
    afterEach(() => jest.restoreAllMocks());

    it('shows initials when no logo is configured', () => {
        render(<PortalLogo name="Partner Developer Portal" src={null} />);

        expect(screen.getByText('PD')).toBeVisible();
    });

    it('shows the loaded logo instead of initials', () => {
        const image = document.createElement('img');
        jest.spyOn(window, 'Image').mockImplementation(() => image);
        render(<PortalLogo name="Partner Portal" src="https://portal.example.com/logo.png" />);

        act(() => image.dispatchEvent(new Event('load')));

        expect(screen.getByRole('img', { name: 'Partner Portal logo' })).toHaveAttribute('src', 'https://portal.example.com/logo.png');
        expect(screen.queryByText('PP')).not.toBeInTheDocument();
    });

    it('keeps initials when loading the logo fails', () => {
        const image = document.createElement('img');
        jest.spyOn(window, 'Image').mockImplementation(() => image);
        render(<PortalLogo name="Partner Portal" src="https://portal.example.com/missing.png" />);

        act(() => image.dispatchEvent(new Event('error')));

        expect(screen.getByText('PP')).toBeVisible();
        expect(screen.queryByRole('img')).not.toBeInTheDocument();
    });
});
