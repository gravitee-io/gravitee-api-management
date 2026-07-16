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
import { Button, Input, Label } from '@gravitee/graphene-core';
import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';

import type { DeveloperPortal } from '../../portals/types';
import { notifySsoSimulated } from '../context/ConsumerAuthProvider';
import { useConsumerAuth } from '../hooks/useConsumerAuth';
import { ConsumerAuthError, loginOrCreateSsoConsumer } from '../services/consumer-auth.service';
import { DEMO_CONSUMER_PASSWORD, DEMO_CONSUMER_USERNAME } from '../storage/seed-demo-consumer';
import type { ConsumerAuthProvider } from '../types/consumer-auth.types';
import { AuthLayout } from './AuthLayout';
import { SsoProviderButtons } from './SsoProviderButtons';
import styles from './AuthForm.module.scss';

interface LoginPageProps {
    readonly portal: DeveloperPortal;
    readonly loginPath: string;
    readonly signupPath: string;
    readonly defaultRedirectPath: string;
    /** When true, successful login updates session in place (editor preview frame). */
    readonly embedded?: boolean;
}

export function LoginPage({
    portal,
    loginPath,
    signupPath,
    defaultRedirectPath,
    embedded = false,
}: LoginPageProps) {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const { login } = useConsumerAuth();
    const [identifier, setIdentifier] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [isPending, setIsPending] = useState(false);

    const redirectPath = searchParams.get('redirect') || defaultRedirectPath;

    const handleSubmit = async (event: React.FormEvent) => {
        event.preventDefault();
        setError(null);
        setIsPending(true);

        try {
            await login(identifier, password);
            if (!embedded) {
                navigate(redirectPath, { replace: true });
            }
        } catch (caught) {
            const message =
                caught instanceof ConsumerAuthError
                    ? caught.message
                    : 'Unable to sign in. Please try again.';
            setError(message);
        } finally {
            setIsPending(false);
        }
    };

    const handleSso = async (provider: ConsumerAuthProvider) => {
        setError(null);
        setIsPending(true);

        try {
            await loginOrCreateSsoConsumer(portal.id, provider);
            notifySsoSimulated(provider);
            if (!embedded) {
                navigate(redirectPath, { replace: true });
            }
        } catch (caught) {
            const message =
                caught instanceof ConsumerAuthError
                    ? caught.message
                    : 'Unable to sign in with SSO.';
            setError(message);
        } finally {
            setIsPending(false);
        }
    };

    return (
        <AuthLayout portal={portal} title="Welcome back" subtitle="Sign in to access your developer portal">
            <form className={styles.form} onSubmit={event => void handleSubmit(event)}>
                <div className={styles.field}>
                    <Label htmlFor="login-identifier">Email or username</Label>
                    <Input
                        id="login-identifier"
                        autoComplete="username"
                        value={identifier}
                        onChange={event => setIdentifier(event.target.value)}
                        required
                    />
                </div>

                <div className={styles.field}>
                    <Label htmlFor="login-password">Password</Label>
                    <Input
                        id="login-password"
                        type="password"
                        autoComplete="current-password"
                        value={password}
                        onChange={event => setPassword(event.target.value)}
                        required
                    />
                </div>

                {error && (
                    <p className={styles.error} role="alert">
                        {error}
                    </p>
                )}

                <p className={styles.hint}>
                    Demo credentials: <strong>{DEMO_CONSUMER_USERNAME}</strong> / <strong>{DEMO_CONSUMER_PASSWORD}</strong>
                </p>

                <Button type="submit" disabled={isPending} className="w-full">
                    {isPending ? 'Signing in…' : 'Sign in'}
                </Button>
            </form>

            <div className={styles.divider}>or continue with</div>

            <SsoProviderButtons disabled={isPending} onProviderClick={provider => void handleSso(provider)} />

            <div className={styles.footer}>
                <span>Don&apos;t have an account? </span>
                <Link className={styles.link} to={`${signupPath}?redirect=${encodeURIComponent(redirectPath)}`}>
                    Sign up
                </Link>
            </div>
        </AuthLayout>
    );
}
