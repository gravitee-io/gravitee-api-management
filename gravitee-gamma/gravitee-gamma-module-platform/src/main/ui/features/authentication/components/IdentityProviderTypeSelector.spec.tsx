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

import { renderWithGraphene } from '@gravitee/graphene-core/testing';
import { fireEvent, screen } from '@testing-library/react';

import { IdentityProviderTypeSelector } from './IdentityProviderTypeSelector';

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

describe('IdentityProviderTypeSelector', () => {
    it('selects a provider type', () => {
        const onChange = jest.fn();
        renderWithGraphene(<IdentityProviderTypeSelector value="GRAVITEEIO_AM" hasOpenIdConnectLicense onChange={onChange} />);
        fireEvent.click(screen.getByRole('radio', { name: /^Google$/i }));
        expect(onChange).toHaveBeenCalledWith('GOOGLE');
    });

    it('does not reset when the already selected type is clicked', () => {
        const onChange = jest.fn();
        renderWithGraphene(<IdentityProviderTypeSelector value="GRAVITEEIO_AM" hasOpenIdConnectLicense onChange={onChange} />);
        fireEvent.click(screen.getByRole('radio', { name: /Gravitee.io AM/i }));
        expect(onChange).not.toHaveBeenCalled();
    });

    it('opens the license dialog instead of selecting OIDC when the feature is locked', () => {
        const onChange = jest.fn();
        renderWithGraphene(<IdentityProviderTypeSelector value="GRAVITEEIO_AM" hasOpenIdConnectLicense={false} onChange={onChange} />);
        fireEvent.click(screen.getByRole('radio', { name: /OpenID Connect/i }));
        expect(onChange).not.toHaveBeenCalled();
        expect(screen.getByText('OpenID Connect SSO')).not.toBeNull();
        expect(screen.getByRole('link', { name: 'Start a free trial' })).not.toBeNull();
    });

    it('names locked OpenID Connect as requiring an enterprise license', () => {
        renderWithGraphene(<IdentityProviderTypeSelector value="GRAVITEEIO_AM" hasOpenIdConnectLicense={false} onChange={jest.fn()} />);
        expect(screen.getByRole('radio', { name: 'OpenID Connect, requires an enterprise license' })).not.toBeNull();
        expect(screen.getByRole('radio', { name: /^Google$/i }).getAttribute('aria-label')).toBeNull();
    });

    it('moves focus with arrow keys without changing the selected type', () => {
        const onChange = jest.fn();
        renderWithGraphene(<IdentityProviderTypeSelector value="GRAVITEEIO_AM" hasOpenIdConnectLicense onChange={onChange} />);
        fireEvent.keyDown(screen.getByRole('radio', { name: /Gravitee.io AM/i }), { key: 'ArrowRight' });
        expect(onChange).not.toHaveBeenCalled();
        expect(screen.getByRole('radio', { name: /Gravitee.io AM/i }).getAttribute('aria-checked')).toBe('true');
        expect(document.activeElement).toBe(screen.getByRole('radio', { name: /OpenID Connect/i }));
    });

    it('moves focus onto locked OIDC without selecting it or opening the license dialog', () => {
        const onChange = jest.fn();
        renderWithGraphene(<IdentityProviderTypeSelector value="GRAVITEEIO_AM" hasOpenIdConnectLicense={false} onChange={onChange} />);
        fireEvent.keyDown(screen.getByRole('radio', { name: /Gravitee.io AM/i }), { key: 'ArrowRight' });
        expect(onChange).not.toHaveBeenCalled();
        expect(screen.queryByText('OpenID Connect SSO')).toBeNull();
        expect(screen.getByRole('radio', { name: /Gravitee.io AM/i }).getAttribute('aria-checked')).toBe('true');
        expect(document.activeElement).toBe(screen.getByRole('radio', { name: /OpenID Connect/i }));
    });

    it('does not change type when arrowing past locked OpenID Connect onto Google', () => {
        const onChange = jest.fn();
        renderWithGraphene(<IdentityProviderTypeSelector value="GRAVITEEIO_AM" hasOpenIdConnectLicense={false} onChange={onChange} />);
        fireEvent.keyDown(screen.getByRole('radio', { name: /Gravitee.io AM/i }), { key: 'ArrowRight' });
        fireEvent.keyDown(screen.getByRole('radio', { name: /OpenID Connect/i }), { key: 'ArrowRight' });
        expect(onChange).not.toHaveBeenCalled();
        expect(screen.getByRole('radio', { name: /Gravitee.io AM/i }).getAttribute('aria-checked')).toBe('true');
        expect(document.activeElement).toBe(screen.getByRole('radio', { name: /^Google$/i }));
    });

    it('does not commit when a click is delivered in the same turn as arrow navigation', () => {
        const onChange = jest.fn();
        renderWithGraphene(<IdentityProviderTypeSelector value="GRAVITEEIO_AM" hasOpenIdConnectLicense onChange={onChange} />);
        fireEvent.keyDown(screen.getByRole('radio', { name: /Gravitee.io AM/i }), { key: 'ArrowRight' });
        fireEvent.click(screen.getByRole('radio', { name: /OpenID Connect/i }));
        expect(onChange).not.toHaveBeenCalled();
        expect(screen.getByRole('radio', { name: /Gravitee.io AM/i }).getAttribute('aria-checked')).toBe('true');
    });

    it('commits the focused type on Enter', () => {
        const onChange = jest.fn();
        renderWithGraphene(<IdentityProviderTypeSelector value="GRAVITEEIO_AM" hasOpenIdConnectLicense={false} onChange={onChange} />);
        fireEvent.keyDown(screen.getByRole('radio', { name: /Gravitee.io AM/i }), { key: 'ArrowRight' });
        fireEvent.keyDown(screen.getByRole('radio', { name: /OpenID Connect/i }), { key: 'ArrowRight' });
        fireEvent.keyDown(screen.getByRole('radio', { name: /^Google$/i }), { key: 'Enter' });
        expect(onChange).toHaveBeenCalledWith('GOOGLE');
    });

    it('opens the license dialog when Space is pressed on locked OpenID Connect', () => {
        const onChange = jest.fn();
        renderWithGraphene(<IdentityProviderTypeSelector value="GRAVITEEIO_AM" hasOpenIdConnectLicense={false} onChange={onChange} />);
        fireEvent.keyDown(screen.getByRole('radio', { name: /Gravitee.io AM/i }), { key: 'ArrowRight' });
        fireEvent.keyDown(screen.getByRole('radio', { name: /OpenID Connect/i }), { key: ' ' });
        expect(onChange).not.toHaveBeenCalled();
        expect(screen.getByText('OpenID Connect SSO')).not.toBeNull();
    });

    it('keeps locked OpenID Connect enabled so it can open the license dialog', () => {
        renderWithGraphene(<IdentityProviderTypeSelector value="GRAVITEEIO_AM" hasOpenIdConnectLicense={false} onChange={jest.fn()} />);
        expect(screen.getByRole('radio', { name: /OpenID Connect/i }).getAttribute('aria-disabled')).toBeNull();
    });
});
