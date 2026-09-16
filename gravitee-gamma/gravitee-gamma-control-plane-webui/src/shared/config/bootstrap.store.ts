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
import { create } from 'zustand';
import { devtools } from 'zustand/middleware';

import { isLocalLoginEnabled, type LocalLoginConsoleSettings } from '../../../../gamma-ui-shared/src/consoleSettings';
import type { SocialIdentityProvider } from '../../features/auth/auth.types';

export interface BootstrapConfig {
    managementBaseURL: string;
    gammaBaseURL: string;
    organizationId: string;
    identityProviders: SocialIdentityProvider[];
    localLoginEnabled: boolean;
    registrationEnabled: boolean;
    automaticValidationEnabled: boolean;
}

interface BootstrapState {
    config: BootstrapConfig | null;
    loading: boolean;
    error: Error | null;
    loginMethodsFetchedAt: number | null;
    initialize: () => Promise<void>;
    refreshLoginMethods: () => Promise<void>;
}

/** Skip a LoginPage refetch when bootstrap just loaded the same login-method APIs. */
const LOGIN_METHODS_FRESH_MS = 30_000;

let latestLoginMethodsRefreshId = 0;

function sanitizeBaseURL(url: string): string {
    return url.endsWith('/') ? url.slice(0, -1) : url;
}

function organizationManagementUrl(managementBaseURL: string, organizationId: string): string {
    return `${sanitizeBaseURL(managementBaseURL)}/organizations/${organizationId}`;
}

type ConsoleAccessSettings = Pick<BootstrapConfig, 'localLoginEnabled' | 'registrationEnabled' | 'automaticValidationEnabled'>;

interface RegistrationConsoleSettings {
    management?: {
        userCreation?: { enabled?: boolean };
        automaticValidation?: { enabled?: boolean };
    };
}

/** Until `/console` has been read once, every way in that it controls stays shut. */
const UNREAD_CONSOLE_ACCESS_SETTINGS: ConsoleAccessSettings = {
    localLoginEnabled: false,
    registrationEnabled: false,
    automaticValidationEnabled: false,
};

/** A body that is not an object counts as unread, like a failed request, so it cannot reopen a gate. */
function consoleAccessSettingsFrom(consoleJson: unknown): ConsoleAccessSettings | undefined {
    if (!consoleJson || typeof consoleJson !== 'object') {
        return undefined;
    }
    const { management } = consoleJson as RegistrationConsoleSettings;
    // Unlike local login, registration defaults to off: only an explicit `true` opens sign-up.
    return {
        localLoginEnabled: isLocalLoginEnabled(consoleJson as LocalLoginConsoleSettings),
        registrationEnabled: management?.userCreation?.enabled === true,
        automaticValidationEnabled: management?.automaticValidation?.enabled === true,
    };
}

async function fetchIdentityProviders(managementBaseURL: string, organizationId: string): Promise<SocialIdentityProvider[] | undefined> {
    try {
        const idpRes = await fetch(`${organizationManagementUrl(managementBaseURL, organizationId)}/social-identities`);
        if (idpRes.ok) {
            return (await idpRes.json()) as SocialIdentityProvider[];
        }
    } catch {
        // Non-fatal: keep the previous list, or an empty list on first load.
    }
    return undefined;
}

async function fetchConsoleAccessSettings(managementBaseURL: string, organizationId: string): Promise<ConsoleAccessSettings | undefined> {
    try {
        const consoleRes = await fetch(`${organizationManagementUrl(managementBaseURL, organizationId)}/console`);
        if (consoleRes.ok) {
            return consoleAccessSettingsFrom(await consoleRes.json());
        }
    } catch {
        // Non-fatal: keep the previous values, or leave local login and registration off until a successful read.
    }
    return undefined;
}

async function loadLoginMethods(
    managementBaseURL: string,
    organizationId: string,
): Promise<{ identityProviders: SocialIdentityProvider[] | undefined; consoleSettings: ConsoleAccessSettings | undefined }> {
    const [identityProviders, consoleSettings] = await Promise.all([
        fetchIdentityProviders(managementBaseURL, organizationId),
        fetchConsoleAccessSettings(managementBaseURL, organizationId),
    ]);
    return { identityProviders, consoleSettings };
}

function isLoginMethodsFresh(fetchedAt: number | null): boolean {
    return fetchedAt !== null && Date.now() - fetchedAt < LOGIN_METHODS_FRESH_MS;
}

export const useBootstrapStore = create<BootstrapState>()(
    devtools(
        (set, get) => ({
            config: null,
            loading: false,
            error: null,
            loginMethodsFetchedAt: null,

            initialize: async () => {
                if (get().config || get().loading) return;
                set({ loading: true, error: null });

                try {
                    const constantsRes = await fetch('/constants.json');
                    if (!constantsRes.ok) throw new Error(`Failed to fetch constants.json: ${constantsRes.status}`);
                    const constants = await constantsRes.json();
                    const gammaBaseURL = sanitizeBaseURL(constants.gammaBaseURL);

                    const bootstrapRes = await fetch(`${gammaBaseURL}/ui/bootstrap`);
                    if (!bootstrapRes.ok) throw new Error(`Failed to fetch bootstrap config: ${bootstrapRes.status}`);
                    const bootstrap = await bootstrapRes.json();

                    const managementBaseURL = sanitizeBaseURL(bootstrap.managementBaseURL);
                    const organizationId = bootstrap.organizationId as string;
                    const loginMethods = await loadLoginMethods(managementBaseURL, organizationId);
                    const loginMethodsFetchedAt =
                        loginMethods.identityProviders !== undefined && loginMethods.consoleSettings !== undefined ? Date.now() : null;

                    set({
                        config: {
                            managementBaseURL,
                            gammaBaseURL: sanitizeBaseURL(bootstrap.gammaBaseURL),
                            organizationId,
                            identityProviders: loginMethods.identityProviders ?? [],
                            ...(loginMethods.consoleSettings ?? UNREAD_CONSOLE_ACCESS_SETTINGS),
                        },
                        loginMethodsFetchedAt,
                        loading: false,
                    });
                } catch (error) {
                    set({ error: error instanceof Error ? error : new Error(String(error)), loading: false });
                    throw error;
                }
            },

            refreshLoginMethods: async () => {
                const config = get().config;
                if (!config) {
                    return;
                }
                if (isLoginMethodsFresh(get().loginMethodsFetchedAt)) {
                    return;
                }

                const requestId = ++latestLoginMethodsRefreshId;
                const [identityProviders, consoleSettings] = await Promise.all([
                    fetchIdentityProviders(config.managementBaseURL, config.organizationId),
                    fetchConsoleAccessSettings(config.managementBaseURL, config.organizationId),
                ]);
                if (requestId !== latestLoginMethodsRefreshId) {
                    return;
                }

                const current = get().config;
                if (!current) {
                    return;
                }

                set({
                    config: {
                        ...current,
                        identityProviders: identityProviders ?? current.identityProviders,
                        localLoginEnabled: consoleSettings?.localLoginEnabled ?? current.localLoginEnabled,
                        registrationEnabled: consoleSettings?.registrationEnabled ?? current.registrationEnabled,
                        automaticValidationEnabled: consoleSettings?.automaticValidationEnabled ?? current.automaticValidationEnabled,
                    },
                    loginMethodsFetchedAt:
                        identityProviders !== undefined && consoleSettings !== undefined ? Date.now() : get().loginMethodsFetchedAt,
                });
            },
        }),
        { name: 'bootstrap' },
    ),
);
