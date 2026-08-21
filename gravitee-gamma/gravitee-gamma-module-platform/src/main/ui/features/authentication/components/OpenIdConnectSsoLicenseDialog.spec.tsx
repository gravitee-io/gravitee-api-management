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

import { fireEvent, render, screen } from '@testing-library/react';

import { OpenIdConnectSsoLicenseDialog } from './OpenIdConnectSsoLicenseDialog';

describe('OpenIdConnectSsoLicenseDialog', () => {
    it('shows upgrade copy and a trial link when open', () => {
        render(<OpenIdConnectSsoLicenseDialog open onOpenChange={jest.fn()} />);

        expect(screen.getByText('OpenID Connect SSO')).not.toBeNull();
        expect(screen.getByText(/part of Gravitee Enterprise/i)).not.toBeNull();
        expect(screen.getByRole('link', { name: 'Start a free trial' }).getAttribute('href')).toBe('https://gravitee.io/self-hosted-trial');
    });

    it('notifies the parent when Close is clicked', () => {
        const onOpenChange = jest.fn();
        render(<OpenIdConnectSsoLicenseDialog open onOpenChange={onOpenChange} />);

        fireEvent.click(screen.getAllByRole('button', { name: 'Close' })[0]);
        expect(onOpenChange).toHaveBeenCalledWith(false);
    });
});
