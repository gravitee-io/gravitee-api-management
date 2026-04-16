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
import {
    Alert,
    AlertDescription,
    AlertTitle,
    Button,
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
    Field,
    FieldLabel,
    Input,
    Separator,
    Spinner,
    cn,
} from '@gravitee/graphene-core';
import { useState, type SubmitEvent } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

import { useIdentityProviders, useLogin } from '../auth.selectors';
import { useAuthStore } from '../auth.store';
import type { SocialIdentityProvider } from '../auth.types';
import { getProviderTextColor } from '../idp.utils';
import { IdpIcon } from './IdpIcons';

export function LoginPage() {
    const login = useLogin();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const identityProviders = useIdentityProviders();
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

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

    return (
        <div className={cn('flex min-h-screen flex-col items-center justify-center p-4', 'font-sans text-foreground')}>
            <Card className="w-full max-w-md shadow-md">
                <CardHeader className="space-y-1 text-center">
                    <CardTitle className="text-2xl">Sign in</CardTitle>
                    <CardDescription>Gravitee Gamma — enter your account credentials to continue.</CardDescription>
                </CardHeader>
                <CardContent>
                    <form onSubmit={handleSubmit} className="space-y-4">
                        {error ? (
                            <Alert variant="destructive" role="alert">
                                <AlertTitle>Could not sign in</AlertTitle>
                                <AlertDescription>{error}</AlertDescription>
                            </Alert>
                        ) : null}

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

                    {identityProviders.length > 0 && (
                        <>
                            <div className="my-4 flex items-center gap-3">
                                <Separator className="flex-1" />
                                <span className="text-muted-foreground text-sm">or</span>
                                <Separator className="flex-1" />
                            </div>

                            <div className="flex flex-col gap-3">
                                {identityProviders.map(provider => (
                                    <IdpButton key={provider.id} provider={provider} onClick={() => handleIdpLogin(provider.id)} />
                                ))}
                            </div>
                        </>
                    )}
                </CardContent>
            </Card>
        </div>
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
