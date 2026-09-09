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
    Field,
    FieldLabel,
    Input,
    PasswordInput,
    Separator,
    Spinner,
} from '@gravitee/graphene-core';
import { useEffect, useState, type SubmitEvent } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

import { useBootstrapStore } from '../../../shared/config/bootstrap.store';
import { useIdentityProviders, useLocalLoginEnabled, useLogin } from '../auth.selectors';
import { useAuthStore } from '../auth.store';
import type { SocialIdentityProvider } from '../auth.types';
import { AuthPageShell } from './AuthPageShell';
import { IdpIcon } from './IdpIcons';

const NO_LOGIN_METHOD_MESSAGE = 'No sign-in method is configured. Contact your administrator.';
const HEX_COLOR = /^#(?:[0-9a-f]{3}|[0-9a-f]{6}|[0-9a-f]{8})$/i;
const SSO_LOGIN_DESCRIPTION = 'Continue with your identity provider.';

/**
 * Only the identity-provider route needs a supporting line. With local login the two
 * labelled fields below say everything, and when sign-in is unavailable the alert does.
 */
function loginCardDescription(noLoginMethod: boolean, localLoginEnabled: boolean): string | undefined {
    if (localLoginEnabled || noLoginMethod) {
        return undefined;
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
            setError("That username and password don't match. Try again.");
        } finally {
            setLoading(false);
        }
    };

    const handleIdpLogin = async (providerId: string) => {
        const redirect = searchParams.get('redirect') || '/';
        try {
            await useAuthStore.getState().loginWithProvider(providerId, redirect);
        } catch {
            setError("Couldn't reach that identity provider. Try again.");
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
                        <PasswordInput
                            id="login-password"
                            name="password"
                            value={password}
                            onChange={event => setPassword(event.target.value)}
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

/**
 * One row per provider: the provider glyph on a neutral chip, then the name.
 *
 * The colour is configured per provider so an organization can tell two authenticators of the
 * same type apart — two Okta tenants, staff versus contractors — so it has to survive. It rings
 * the chip rather than filling it. Filling it means the glyph sits on a colour an administrator
 * chose, and a multi-colour mark cannot adapt to that: Google's blue segment disappears on a
 * blue chip. A neutral chip renders every glyph legibly whatever colour is configured, and the
 * monochrome marks inherit the button's own foreground, so both themes work with no computation.
 */
function IdpButton({ provider, onClick }: { provider: SocialIdentityProvider; onClick: () => void }) {
    // Provider colour is runtime data, not a design decision, so it cannot come from a token.
    // Only a hex colour is taken: `border-color` also accepts per-side lists and `var()`
    // references, and a value an administrator typed has no business being either.
    const chipStyle = HEX_COLOR.test(provider.color ?? '') ? { borderColor: provider.color } : undefined;

    return (
        <Button
            type="button"
            variant="outline"
            size="lg"
            // 40px, a step above Graphene's tallest button (h-9), settled with design: a provider
            // row reads as a list item rather than an action. The sign-in button above stays at
            // h-9 — the two were reviewed together on the card that stacks them.
            className="h-10 w-full justify-start gap-3"
            onClick={onClick}
            aria-label={`Continue with ${provider.name}`}
        >
            {/* A bordered `bg-muted` well, after Graphene's `itemMediaVariants` `icon`, at the 24px
                design settled on. The border is load-bearing — `Button`'s outline variant hovers to
                `bg-muted`, so without it the chip disappears under the pointer — and it is what
                carries the provider colour: a second inset ring inside it read as a double stroke,
                and ate the corner, because an inset shadow does not follow the radius the way a
                border does. `rounded-lg` is the 4px component shape DESIGN.md §5 asks for;
                `ItemMedia` says `rounded-sm`, which resolves to 0 against Graphene's 2px `--radius`. */}
            <span aria-hidden className="flex size-6 shrink-0 items-center justify-center rounded-lg border bg-muted" style={chipStyle}>
                <IdpIcon type={provider.type} className="size-4" />
            </span>
            <span className="truncate">{provider.name}</span>
        </Button>
    );
}
