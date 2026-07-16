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
import { ConsumerAuthError, signupConsumer } from '../services/consumer-auth.service';
import { CONSUMER_USE_CASE_OPTIONS, type ConsumerUseCase } from '../types/consumer-auth.types';
import { AuthLayout } from './AuthLayout';
import styles from './AuthForm.module.scss';

interface SignupPageProps {
    readonly portal: DeveloperPortal;
    readonly loginPath: string;
    readonly defaultRedirectPath: string;
}

export function SignupPage({ portal, loginPath, defaultRedirectPath }: SignupPageProps) {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [company, setCompany] = useState('');
    const [jobTitle, setJobTitle] = useState('');
    const [useCase, setUseCase] = useState<ConsumerUseCase | ''>('');
    const [acceptedTerms, setAcceptedTerms] = useState(false);
    const [marketingOptIn, setMarketingOptIn] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [isPending, setIsPending] = useState(false);
    const [submittedEmail, setSubmittedEmail] = useState<string | null>(null);

    const redirectPath = searchParams.get('redirect') || defaultRedirectPath;

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
            await signupConsumer(portal.id, {
                firstName,
                lastName,
                email,
                password,
                company,
                jobTitle: jobTitle || undefined,
                useCase: useCase || undefined,
                marketingOptIn,
            });
            setSubmittedEmail(email.trim().toLowerCase());
        } catch (caught) {
            const message =
                caught instanceof ConsumerAuthError
                    ? caught.message
                    : 'Unable to create your account. Please try again.';
            setError(message);
        } finally {
            setIsPending(false);
        }
    };

    if (submittedEmail) {
        return (
            <AuthLayout portal={portal} title="Check your email">
                <p className={styles.successMessage}>
                    We&apos;ve sent you a confirmation link to:
                </p>
                <p className={styles.successEmail}>{submittedEmail}</p>
                <p className={styles.successMessage}>
                    For this POC your account is already active — you can sign in right away.
                </p>
                <Button
                    className="mt-4 w-full"
                    onClick={() => navigate(redirectPath, { replace: true })}
                >
                    Continue to portal
                </Button>
                <div className={styles.footer}>
                    <Link className={styles.link} to={loginPath}>
                        Back to sign in
                    </Link>
                </div>
            </AuthLayout>
        );
    }

    return (
        <AuthLayout portal={portal} title="Create your account" subtitle="Join the developer community">
            <form className={styles.form} onSubmit={event => void handleSubmit(event)}>
                <div className={styles.field}>
                    <Label htmlFor="signup-first-name">First name</Label>
                    <Input
                        id="signup-first-name"
                        value={firstName}
                        onChange={event => setFirstName(event.target.value)}
                        required
                    />
                </div>

                <div className={styles.field}>
                    <Label htmlFor="signup-last-name">Last name</Label>
                    <Input
                        id="signup-last-name"
                        value={lastName}
                        onChange={event => setLastName(event.target.value)}
                        required
                    />
                </div>

                <div className={styles.field}>
                    <Label htmlFor="signup-email">Work email</Label>
                    <Input
                        id="signup-email"
                        type="email"
                        autoComplete="email"
                        value={email}
                        onChange={event => setEmail(event.target.value)}
                        required
                    />
                </div>

                <div className={styles.field}>
                    <Label htmlFor="signup-company">Company / organization</Label>
                    <Input
                        id="signup-company"
                        value={company}
                        onChange={event => setCompany(event.target.value)}
                        required
                    />
                </div>

                <div className={styles.field}>
                    <Label htmlFor="signup-job-title">Job title (optional)</Label>
                    <Input
                        id="signup-job-title"
                        value={jobTitle}
                        onChange={event => setJobTitle(event.target.value)}
                    />
                </div>

                <div className={styles.field}>
                    <Label htmlFor="signup-use-case">What are you building? (optional)</Label>
                    <select
                        id="signup-use-case"
                        className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                        value={useCase}
                        onChange={event => setUseCase(event.target.value as ConsumerUseCase | '')}
                    >
                        <option value="">Select an option</option>
                        {CONSUMER_USE_CASE_OPTIONS.map(option => (
                            <option key={option.value} value={option.value}>
                                {option.label}
                            </option>
                        ))}
                    </select>
                </div>

                <div className={styles.field}>
                    <Label htmlFor="signup-password">Password</Label>
                    <Input
                        id="signup-password"
                        type="password"
                        autoComplete="new-password"
                        value={password}
                        onChange={event => setPassword(event.target.value)}
                        required
                    />
                </div>

                <div className={styles.field}>
                    <Label htmlFor="signup-confirm-password">Confirm password</Label>
                    <Input
                        id="signup-confirm-password"
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

                <label className={styles.checkboxRow}>
                    <input
                        type="checkbox"
                        checked={marketingOptIn}
                        onChange={event => setMarketingOptIn(event.target.checked)}
                    />
                    <span>Send me product updates and developer tips (optional)</span>
                </label>

                {error && (
                    <p className={styles.error} role="alert">
                        {error}
                    </p>
                )}

                <Button type="submit" disabled={isPending} className="w-full">
                    {isPending ? 'Creating account…' : 'Sign up'}
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
