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
import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import type { DeveloperPortal } from '../../portals/types';
import { getPortalTenant } from '../../tenants/storage/portal-tenants.storage';
import { useConsumerAuth } from '../hooks/useConsumerAuth';
import { acceptInvitation, ConsumerAuthError } from '../services/consumer-auth.service';
import { getInvitationByToken } from '../storage/portal-invitations.storage';
import type { PortalInvitation } from '../types/consumer-auth.types';
import { AuthLayout } from './AuthLayout';
import styles from './AuthForm.module.scss';

interface InviteAcceptPageProps {
    readonly portal: DeveloperPortal;
    readonly token: string;
    readonly loginPath: string;
    readonly signupPath: string;
    readonly defaultRedirectPath: string;
}

export function InviteAcceptPage({
    portal,
    token,
    loginPath,
    signupPath,
    defaultRedirectPath,
}: InviteAcceptPageProps) {
    const navigate = useNavigate();
    const { refreshSession } = useConsumerAuth();
    const [invitation, setInvitation] = useState<PortalInvitation | null>(null);
    const [tenantName, setTenantName] = useState('');
    const [loadingInvitation, setLoadingInvitation] = useState(true);
    const [tokenInvalid, setTokenInvalid] = useState(false);
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [company, setCompany] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [acceptedTerms, setAcceptedTerms] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [isPending, setIsPending] = useState(false);
    const [submitted, setSubmitted] = useState(false);

    useEffect(() => {
        void (async () => {
            const found = await getInvitationByToken(token);
            if (!found || found.portalId !== portal.id || found.acceptedAt) {
                setTokenInvalid(true);
                setLoadingInvitation(false);
                return;
            }

            const tenant = await getPortalTenant(found.tenantId);
            setInvitation(found);
            setTenantName(tenant?.name ?? 'your team');
            setLoadingInvitation(false);
        })();
    }, [portal.id, token]);

    const handleSubmit = async (event: React.FormEvent) => {
        event.preventDefault();
        setError(null);

        if (password !== confirmPassword) {
            setError('Passwords do not match.');
            return;
        }

        if (!acceptedTerms) {
            setError('You must accept the terms of service.');
            return;
        }

        setIsPending(true);

        try {
            await acceptInvitation(portal.id, {
                token,
                firstName,
                lastName,
                password,
                company: company || undefined,
            });
            await refreshSession();
            setSubmitted(true);
        } catch (caught) {
            const message =
                caught instanceof ConsumerAuthError
                    ? caught.message
                    : 'Unable to accept the invitation. Please try again.';
            setError(message);
        } finally {
            setIsPending(false);
        }
    };

    if (loadingInvitation) {
        return (
            <AuthLayout portal={portal} title="Loading invitation…">
                <p className={styles.hint}>Please wait while we verify your invitation.</p>
            </AuthLayout>
        );
    }

    if (tokenInvalid) {
        return (
            <AuthLayout portal={portal} title="Invalid invitation">
                <p className={styles.error} role="alert">
                    This invitation link is invalid or has already been used.
                </p>
                <div className={styles.footer}>
                    <Link className={styles.link} to={loginPath}>
                        Sign in
                    </Link>
                    {' · '}
                    <Link className={styles.link} to={signupPath}>
                        Sign up
                    </Link>
                </div>
            </AuthLayout>
        );
    }

    if (submitted) {
        return (
            <AuthLayout portal={portal} title="Invitation accepted">
                <p className={styles.successMessage}>
                    Your account has been created. You are now signed in and can access the portal.
                </p>
                <Button className="mt-4 w-full" onClick={() => navigate(defaultRedirectPath, { replace: true })}>
                    Go to portal
                </Button>
            </AuthLayout>
        );
    }

    return (
        <AuthLayout
            portal={portal}
            title="Accept your invitation"
            subtitle={`You've been invited to join ${tenantName}`}
        >
            <form className={styles.form} onSubmit={event => void handleSubmit(event)}>
                <div className={styles.field}>
                    <Label htmlFor="invite-first-name">First name</Label>
                    <Input
                        id="invite-first-name"
                        value={firstName}
                        onChange={event => setFirstName(event.target.value)}
                        required
                    />
                </div>

                <div className={styles.field}>
                    <Label htmlFor="invite-last-name">Last name</Label>
                    <Input
                        id="invite-last-name"
                        value={lastName}
                        onChange={event => setLastName(event.target.value)}
                        required
                    />
                </div>

                <div className={styles.field}>
                    <Label htmlFor="invite-email">Email</Label>
                    <Input id="invite-email" type="email" value={invitation?.email ?? ''} readOnly disabled />
                </div>

                <div className={styles.field}>
                    <Label htmlFor="invite-company">Company (optional)</Label>
                    <Input
                        id="invite-company"
                        value={company}
                        onChange={event => setCompany(event.target.value)}
                    />
                </div>

                <div className={styles.field}>
                    <Label htmlFor="invite-password">Password</Label>
                    <Input
                        id="invite-password"
                        type="password"
                        autoComplete="new-password"
                        value={password}
                        onChange={event => setPassword(event.target.value)}
                        required
                    />
                </div>

                <div className={styles.field}>
                    <Label htmlFor="invite-confirm-password">Confirm password</Label>
                    <Input
                        id="invite-confirm-password"
                        type="password"
                        autoComplete="new-password"
                        value={confirmPassword}
                        onChange={event => setConfirmPassword(event.target.value)}
                        required
                    />
                </div>

                <label className={styles.checkboxRow}>
                    <input
                        type="checkbox"
                        checked={acceptedTerms}
                        onChange={event => setAcceptedTerms(event.target.checked)}
                    />
                    <span>I agree to the terms of service and privacy policy</span>
                </label>

                {error && (
                    <p className={styles.error} role="alert">
                        {error}
                    </p>
                )}

                <Button type="submit" disabled={isPending} className="w-full">
                    {isPending ? 'Accepting…' : 'Accept invitation'}
                </Button>
            </form>

            <div className={styles.footer}>
                <span>Already have an account? </span>
                <Link className={styles.link} to={loginPath}>
                    Sign in
                </Link>
            </div>
        </AuthLayout>
    );
}
