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
    Spinner,
    cn,
} from '@gravitee/graphene-core';
import { useMemo, useState, type SubmitEvent } from 'react';
import { Link, useParams } from 'react-router-dom';

import { PasswordRequirements, isPasswordPolicySatisfied } from '../../../shared/password-policy';
import { usePasswordPolicy } from '../hooks/usePasswordPolicy';
import { finalizeResetPassword } from '../services/resetPassword.service';
import { isResetPasswordTokenExpired, parseResetPasswordToken } from '../utils/resetPasswordToken';

function passwordsMatch(password: string, confirmPassword: string): boolean {
    return password.length > 0 && password === confirmPassword;
}

export function ResetPasswordPage() {
    const { token = '' } = useParams<{ token: string }>();
    const tokenClaims = useMemo(() => parseResetPasswordToken(token), [token]);
    const tokenError = useMemo(() => {
        if (!token) {
            return 'Invalid password reset token!';
        }
        if (!tokenClaims) {
            return 'Invalid password reset token!';
        }
        if (isResetPasswordTokenExpired(tokenClaims)) {
            return 'Your password reset token has expired!';
        }
        if (!tokenClaims.sub) {
            return 'Invalid password reset token!';
        }
        return null;
    }, [token, tokenClaims]);

    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [error, setError] = useState('');
    const [success, setSuccess] = useState(false);
    const [loading, setLoading] = useState(false);
    const { policy: passwordPolicy, loading: passwordPolicyLoading, error: passwordPolicyError } = usePasswordPolicy();

    const passwordMismatch = confirmPassword.length > 0 && password !== confirmPassword;
    const passwordPolicySatisfied = isPasswordPolicySatisfied(password, passwordPolicy.rules);
    const canSubmit =
        Boolean(tokenClaims?.sub) &&
        password.length > 0 &&
        confirmPassword.length > 0 &&
        passwordsMatch(password, confirmPassword) &&
        passwordPolicySatisfied &&
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
            !isPasswordPolicySatisfied(password, passwordPolicy.rules)
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

    return (
        <div className={cn('flex min-h-screen flex-col items-center justify-center p-4', 'font-sans text-foreground')}>
            <Card className="w-full max-w-md shadow-md">
                <CardHeader className="space-y-1 text-center">
                    <CardTitle className="text-2xl">Reset password</CardTitle>
                    <CardDescription>to access Gravitee Gamma</CardDescription>
                </CardHeader>
                <CardContent>
                    {tokenError ? (
                        <Alert variant="destructive" role="alert">
                            <AlertTitle>Could not reset password</AlertTitle>
                            <AlertDescription>{tokenError}</AlertDescription>
                        </Alert>
                    ) : null}

                    {success ? (
                        <Alert role="status">
                            <AlertTitle>Password successfully reset</AlertTitle>
                            <AlertDescription>You can now sign in with your new password.</AlertDescription>
                        </Alert>
                    ) : null}

                    {!tokenError && !success ? (
                        <form onSubmit={handleSubmit} className="space-y-4">
                            {passwordPolicyError ? (
                                <Alert variant="destructive" role="alert">
                                    <AlertTitle>Could not load password requirements</AlertTitle>
                                    <AlertDescription>{passwordPolicyError}</AlertDescription>
                                </Alert>
                            ) : null}

                            {error ? (
                                <Alert variant="destructive" role="alert">
                                    <AlertTitle>Reset failed</AlertTitle>
                                    <AlertDescription>{error}</AlertDescription>
                                </Alert>
                            ) : null}

                            <Field orientation="vertical" className="gap-2">
                                <FieldLabel htmlFor="reset-first-name">First name</FieldLabel>
                                <Input id="reset-first-name" value={tokenClaims?.firstname ?? ''} disabled readOnly />
                            </Field>

                            <Field orientation="vertical" className="gap-2">
                                <FieldLabel htmlFor="reset-last-name">Last name</FieldLabel>
                                <Input id="reset-last-name" value={tokenClaims?.lastname ?? ''} disabled readOnly />
                            </Field>

                            <Field orientation="vertical" className="gap-2">
                                <FieldLabel htmlFor="reset-email">Email</FieldLabel>
                                <Input id="reset-email" type="email" value={tokenClaims?.email ?? ''} disabled readOnly />
                            </Field>

                            <Field orientation="vertical" className="gap-2">
                                <FieldLabel htmlFor="reset-password">Password</FieldLabel>
                                <Input
                                    id="reset-password"
                                    type="password"
                                    value={password}
                                    onChange={event => setPassword(event.target.value)}
                                    required
                                    autoComplete="new-password"
                                    // eslint-disable-next-line jsx-a11y/no-autofocus
                                    autoFocus
                                />
                                <PasswordRequirements rules={passwordPolicy.rules} password={password} showStrengthMeter />
                            </Field>

                            <Field orientation="vertical" className="gap-2">
                                <FieldLabel htmlFor="reset-confirm-password">Confirm password</FieldLabel>
                                <Input
                                    id="reset-confirm-password"
                                    type="password"
                                    value={confirmPassword}
                                    onChange={event => setConfirmPassword(event.target.value)}
                                    required
                                    autoComplete="new-password"
                                />
                                {passwordMismatch ? (
                                    <p className="text-sm text-destructive">Password and confirm password must be the same.</p>
                                ) : null}
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
                    ) : null}

                    <p className="mt-4 text-center text-sm text-muted-foreground">
                        Go to{' '}
                        <Link to="/login" className="text-primary underline-offset-4 hover:underline">
                            Sign in
                        </Link>
                    </p>
                </CardContent>
            </Card>
        </div>
    );
}
