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
import { Alert, AlertDescription, AlertTitle, Button, Field, FieldLabel, PasswordInput, Spinner } from '@gravitee/graphene-core';
import { useMemo, useState, type ReactNode, type SubmitEvent } from 'react';
import { Link, useParams } from 'react-router-dom';

import { AuthPageShell } from './AuthPageShell';
import { PasswordRequirements, assessPassword } from '../../../shared/password-policy';
import { usePasswordPolicy } from '../hooks/usePasswordPolicy';
import { finalizeResetPassword } from '../services/resetPassword.service';
import { isAuthTokenExpired, parseAuthToken, type AuthTokenClaims } from '../utils/authToken';

const INVALID_LINK = "This reset link isn't valid. Ask your administrator to send a new one.";
const EXPIRED_LINK = 'This reset link has expired. Ask your administrator to send a new one.';

function passwordsMatch(password: string, confirmPassword: string): boolean {
    return password.length > 0 && password === confirmPassword;
}

/**
 * Why a reset link cannot be used, or null when it can.
 *
 * Expiry is reported before a missing subject so a well-formed link that has simply
 * timed out says so, rather than reading as malformed.
 */
function resetTokenError(token: string, claims: AuthTokenClaims | null): string | null {
    if (!token || !claims) {
        return INVALID_LINK;
    }
    if (isAuthTokenExpired(claims)) {
        return EXPIRED_LINK;
    }
    if (!claims.sub) {
        return INVALID_LINK;
    }
    return null;
}

/**
 * The page's frame, so each of the three outcomes states only its own content.
 *
 * The description instructs; only the branch that can be acted on passes one. Over a dead link
 * or a finished reset it would tell the reader to do something the page is not offering.
 */
function ResetPasswordShell({ description, children }: { description?: string; children: ReactNode }) {
    return (
        <AuthPageShell
            title="Reset password"
            description={description}
            footer={
                <>
                    Back to{' '}
                    <Link to="/login" className="text-primary underline-offset-4 hover:underline">
                        sign in
                    </Link>
                </>
            }
        >
            {children}
        </AuthPageShell>
    );
}

export function ResetPasswordPage() {
    const { token = '' } = useParams<{ token: string }>();
    const tokenClaims = useMemo(() => parseAuthToken(token), [token]);
    const tokenError = useMemo(() => resetTokenError(token, tokenClaims), [token, tokenClaims]);

    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [error, setError] = useState('');
    const [success, setSuccess] = useState(false);
    const [loading, setLoading] = useState(false);
    const { policy: passwordPolicy, loading: passwordPolicyLoading, error: passwordPolicyError } = usePasswordPolicy();

    const accountName = [tokenClaims?.firstname, tokenClaims?.lastname].filter(Boolean).join(' ');
    const passwordMismatch = confirmPassword.length > 0 && password !== confirmPassword;
    // 'undecidable' means the browser cannot read the configured pattern as the server does. Blocking on that
    // would refuse a password the server would accept, so only an outright failure stops submission.
    const policyRefusesPassword = assessPassword(password, passwordPolicy) === 'unsatisfied';
    const canSubmit =
        Boolean(tokenClaims?.sub) &&
        password.length > 0 &&
        confirmPassword.length > 0 &&
        passwordsMatch(password, confirmPassword) &&
        !policyRefusesPassword &&
        !passwordPolicyLoading &&
        !passwordPolicyError &&
        !loading &&
        !tokenError;

    async function handleSubmit(event: SubmitEvent) {
        event.preventDefault();
        if (
            !tokenClaims?.sub ||
            tokenError ||
            passwordPolicyError ||
            passwordPolicyLoading ||
            loading ||
            !passwordsMatch(password, confirmPassword) ||
            policyRefusesPassword
        ) {
            return;
        }

        setError('');
        setLoading(true);
        try {
            await finalizeResetPassword(tokenClaims.sub, {
                token,
                password,
                firstname: tokenClaims.firstname ?? '',
                lastname: tokenClaims.lastname ?? '',
            });
            setSuccess(true);
        } catch (submitError) {
            if (process.env.NODE_ENV !== 'production') {
                console.error('Password reset failed', submitError);
            }
            const message = submitError instanceof Error ? submitError.message : 'An error occurred while resetting your password.';
            setError(message);
        } finally {
            setLoading(false);
        }
    }

    if (tokenError) {
        return (
            <ResetPasswordShell>
                <Alert variant="destructive" role="alert">
                    <AlertTitle>Could not reset password</AlertTitle>
                    <AlertDescription>{tokenError}</AlertDescription>
                </Alert>
            </ResetPasswordShell>
        );
    }

    if (success) {
        return (
            <ResetPasswordShell>
                <Alert role="status">
                    <AlertTitle>Password updated</AlertTitle>
                    <AlertDescription>You can now sign in with your new password.</AlertDescription>
                </Alert>
            </ResetPasswordShell>
        );
    }

    return (
        <ResetPasswordShell description="Choose a new password for your account.">
            <form onSubmit={handleSubmit} className="space-y-4">
                {passwordPolicyError ? (
                    <Alert variant="destructive" role="alert">
                        <AlertTitle>{"Couldn't load password rules"}</AlertTitle>
                        <AlertDescription>{passwordPolicyError}</AlertDescription>
                    </Alert>
                ) : null}

                {error ? (
                    <Alert variant="destructive" role="alert">
                        <AlertTitle>Reset failed</AlertTitle>
                        <AlertDescription>{error}</AlertDescription>
                    </Alert>
                ) : null}

                {/* The account is context, not input. Three disabled fields read as a form the
                reader cannot fill: they fail contrast, keyboard navigation skips them, and
                assistive technology never reaches the name they carry. */}
                {accountName || tokenClaims?.email ? (
                    <div className="text-sm">
                        {accountName ? <p className="font-medium">{accountName}</p> : null}
                        {tokenClaims?.email ? <p className="text-muted-foreground">{tokenClaims.email}</p> : null}
                    </div>
                ) : null}

                <Field orientation="vertical" className="gap-2">
                    <FieldLabel htmlFor="reset-password">Password</FieldLabel>
                    <PasswordInput
                        id="reset-password"
                        value={password}
                        onChange={event => setPassword(event.target.value)}
                        required
                        autoComplete="new-password"
                        // eslint-disable-next-line jsx-a11y/no-autofocus
                        autoFocus
                    />
                    <PasswordRequirements policy={passwordPolicy} password={password} showStrengthMeter />
                </Field>

                <Field orientation="vertical" className="gap-2">
                    <FieldLabel htmlFor="reset-confirm-password">Confirm password</FieldLabel>
                    <PasswordInput
                        id="reset-confirm-password"
                        value={confirmPassword}
                        onChange={event => setConfirmPassword(event.target.value)}
                        required
                        autoComplete="new-password"
                    />
                    {passwordMismatch ? <p className="text-sm text-destructive">Both passwords must match.</p> : null}
                </Field>

                <Button type="submit" className="w-full" size="lg" disabled={!canSubmit}>
                    {loading ? (
                        <span className="inline-flex items-center justify-center gap-2">
                            <Spinner className="size-4 shrink-0" aria-hidden />
                            Resetting password…
                        </span>
                    ) : (
                        'Reset password'
                    )}
                </Button>
            </form>
        </ResetPasswordShell>
    );
}
