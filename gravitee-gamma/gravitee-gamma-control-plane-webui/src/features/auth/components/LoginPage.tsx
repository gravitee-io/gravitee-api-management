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
import { Alert, AlertDescription, AlertTitle, Button, Field, FieldLabel, Input, Separator, Spinner } from '@gravitee/graphene-core';
import { useEffect, useState, type SubmitEvent } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

import { useBootstrapStore } from '../../../shared/config/bootstrap.store';
import { useIdentityProviders, useLocalLoginEnabled, useLogin } from '../auth.selectors';
import { useAuthStore } from '../auth.store';
import type { SocialIdentityProvider } from '../auth.types';
import { getProviderTextColor } from '../idp.utils';
import { AuthPageShell } from './AuthPageShell';
import { IdpIcon } from './IdpIcons';

const NO_LOGIN_METHOD_MESSAGE = 'No login method available. Please contact your administrator.';
const LOCAL_LOGIN_DESCRIPTION = 'Gravitee Gamma — enter your account credentials to continue.';
const SSO_LOGIN_DESCRIPTION = 'Gravitee Gamma — sign in with an identity provider to continue.';
const UNAVAILABLE_LOGIN_DESCRIPTION = 'Gravitee Gamma — sign-in is currently unavailable.';

function loginCardDescription(noLoginMethod: boolean, localLoginEnabled: boolean): string {
    if (localLoginEnabled) {
        return LOCAL_LOGIN_DESCRIPTION;
    }
    if (noLoginMethod) {
        return UNAVAILABLE_LOGIN_DESCRIPTION;
    }
    return SSO_LOGIN_DESCRIPTION;
}

export function LoginPage() {
    const login = useLogin();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const identityProviders = useIdentityProviders();
    const localLoginEnabled = useLocalLoginEnabled();
    const refreshLoginMethods = useBootstrapStore(s => s.refreshLoginMethods);
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        void refreshLoginMethods();
    }, [refreshLoginMethods]);

    const handleSubmit = async (e: SubmitEvent) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        try {
            await login(username, password);
            navigate(searchParams.get('redirect') || '/', { replace: true });
        } catch {
            setError('Login failed! Check username and password.');
        } finally {
            setLoading(false);
        }
    };

    const handleIdpLogin = async (providerId: string) => {
        const redirect = searchParams.get('redirect') || '/';
        try {
            await useAuthStore.getState().loginWithProvider(providerId, redirect);
        } catch {
            setError('Failed to start identity provider authentication.');
        }
    };

    const canSubmit = Boolean(username && password) && !loading;
    const noLoginMethod = !localLoginEnabled && identityProviders.length === 0;
    const displayError = error || (noLoginMethod ? NO_LOGIN_METHOD_MESSAGE : '');
    const description = loginCardDescription(noLoginMethod, localLoginEnabled);

    return (
        <AuthPageShell title="Sign in" description={description}>
            {displayError ? (
                <Alert variant="destructive" role="alert" className="mb-4">
                    <AlertTitle>{error ? 'Could not sign in' : 'Sign-in unavailable'}</AlertTitle>
                    <AlertDescription>{displayError}</AlertDescription>
                </Alert>
            ) : null}

            {localLoginEnabled ? (
                <form onSubmit={handleSubmit} className="space-y-4">
                    <Field orientation="vertical" className="gap-2">
                        <FieldLabel htmlFor="login-username">Username</FieldLabel>
                        <Input
                            id="login-username"
                            name="username"
                            type="text"
                            value={username}
                            onChange={e => setUsername(e.target.value)}
                            required
                            autoComplete="username"
                            placeholder="Enter your username"
                            // eslint-disable-next-line jsx-a11y/no-autofocus
                            autoFocus
                        />
                    </Field>

                    <Field orientation="vertical" className="gap-2">
                        <FieldLabel htmlFor="login-password">Password</FieldLabel>
                        <Input
                            id="login-password"
                            name="password"
                            type="password"
                            value={password}
                            onChange={e => setPassword(e.target.value)}
                            required
                            autoComplete="current-password"
                            placeholder="Enter your password"
                        />
                    </Field>

                    <Button type="submit" className="w-full" size="lg" disabled={!canSubmit}>
                        {loading ? (
                            <span className="inline-flex items-center justify-center gap-2">
                                <Spinner className="size-4 shrink-0" aria-hidden />
                                Signing in…
                            </span>
                        ) : (
                            'Sign in'
                        )}
                    </Button>
                </form>
            ) : null}

            {localLoginEnabled && identityProviders.length > 0 ? (
                <div className="my-4 flex items-center gap-3">
                    <Separator className="flex-1" />
                    <span className="text-muted-foreground text-sm">or</span>
                    <Separator className="flex-1" />
                </div>
            ) : null}

            {identityProviders.length > 0 ? (
                <div className="flex flex-col gap-3">
                    {identityProviders.map(provider => (
                        <IdpButton key={provider.id} provider={provider} onClick={() => handleIdpLogin(provider.id)} />
                    ))}
                </div>
            ) : null}
        </AuthPageShell>
    );
}

function IdpButton({ provider, onClick }: { provider: SocialIdentityProvider; onClick: () => void }) {
    const hasColor = Boolean(provider.color);
    const colorStyle = hasColor ? { backgroundColor: provider.color, color: getProviderTextColor(provider.color) } : {};

    return (
        <Button type="button" variant="outline" className="w-full gap-2" size="lg" style={colorStyle} onClick={onClick}>
            <IdpIcon type={provider.type} className="size-5 shrink-0" />
            <span className="flex-1 text-center">Sign in with {provider.name}</span>
        </Button>
    );
}
