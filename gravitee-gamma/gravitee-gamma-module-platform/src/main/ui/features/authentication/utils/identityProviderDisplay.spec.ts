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

import {
    identityProviderTypeLabel,
    isLocalLoginReadonly,
    LOCAL_LOGIN_LOAD_FAILED_TOOLTIP,
    LOCAL_LOGIN_NEEDS_ACTIVATED_IDP_TOOLTIP,
    LOCAL_LOGIN_NO_PERMISSION_TOOLTIP,
    localLoginSettingTooltip,
    SYSTEM_READONLY_TOOLTIP,
} from './identityProviderDisplay';

describe('identityProviderDisplay', () => {
    it('labels each provider type', () => {
        expect(identityProviderTypeLabel('GRAVITEEIO_AM')).toBe('Gravitee.io AM');
        expect(identityProviderTypeLabel('OIDC')).toBe('OpenID Connect');
        expect(identityProviderTypeLabel('GOOGLE')).toBe('Google');
        expect(identityProviderTypeLabel('GITHUB')).toBe('GitHub');
    });

    it('detects the local-login system readonly lock', () => {
        expect(isLocalLoginReadonly(['console.authentication.localLogin.enabled'])).toBe(true);
        expect(isLocalLoginReadonly([])).toBe(false);
        expect(isLocalLoginReadonly(undefined)).toBe(false);
    });

    it('does not blame a missing identity provider while settings are loading', () => {
        expect(
            localLoginSettingTooltip({
                isLoading: true,
                isError: false,
                canUpdateSettings: true,
                systemReadonly: false,
                hasActivatedIdp: false,
            }),
        ).toBeNull();
    });

    it('explains a load failure instead of asking the operator to activate a provider', () => {
        expect(
            localLoginSettingTooltip({
                isLoading: false,
                isError: true,
                canUpdateSettings: true,
                systemReadonly: false,
                hasActivatedIdp: false,
            }),
        ).toBe(LOCAL_LOGIN_LOAD_FAILED_TOOLTIP);
    });

    it('asks the operator to activate a provider only after settings have loaded', () => {
        expect(
            localLoginSettingTooltip({
                isLoading: false,
                isError: false,
                canUpdateSettings: true,
                systemReadonly: false,
                hasActivatedIdp: false,
            }),
        ).toBe(LOCAL_LOGIN_NEEDS_ACTIVATED_IDP_TOOLTIP);
    });

    it('prefers the system-readonly explanation once settings have loaded', () => {
        expect(
            localLoginSettingTooltip({
                isLoading: false,
                isError: false,
                canUpdateSettings: true,
                systemReadonly: true,
                hasActivatedIdp: false,
            }),
        ).toBe(SYSTEM_READONLY_TOOLTIP);
    });

    it('prefers the system-readonly explanation over a missing permission', () => {
        expect(
            localLoginSettingTooltip({
                isLoading: false,
                isError: false,
                canUpdateSettings: false,
                systemReadonly: true,
                hasActivatedIdp: true,
            }),
        ).toBe(SYSTEM_READONLY_TOOLTIP);
    });

    it('hides the tooltip when local login can be changed', () => {
        expect(
            localLoginSettingTooltip({
                isLoading: false,
                isError: false,
                canUpdateSettings: true,
                systemReadonly: false,
                hasActivatedIdp: true,
            }),
        ).toBeNull();
    });

    it('explains a missing settings permission instead of leaving the toggle unexplained', () => {
        expect(
            localLoginSettingTooltip({
                isLoading: false,
                isError: false,
                canUpdateSettings: false,
                systemReadonly: false,
                hasActivatedIdp: true,
            }),
        ).toBe(LOCAL_LOGIN_NO_PERMISSION_TOOLTIP);
    });
});
