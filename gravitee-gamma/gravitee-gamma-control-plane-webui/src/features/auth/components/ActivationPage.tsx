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
    FieldError,
    FieldLabel,
    PasswordInput,
    Spinner,
} from '@gravitee/graphene-core';
import { useMemo, useState, type ReactNode, type SubmitEvent } from 'react';
import { Link, useParams } from 'react-router-dom';

import { AuthPageShell } from './AuthPageShell';
import { PasswordRequirements, assessPassword } from '../../../shared/password-policy';
import { usePasswordPolicy } from '../hooks/usePasswordPolicy';
import { finalizeRegistration } from '../services/registration.service';
import { isAuthTokenExpired, parseAuthToken, type AuthTokenClaims } from '../utils/authToken';

const INVALID_LINK = "This activation link isn't valid. Ask your administrator to send a new one.";
const EXPIRED_LINK = 'This activation link has expired. Ask your administrator to send a new one.';
const UNUSABLE_LINK = "This activation link can't be used anymore. Ask your administrator to send a new one.";
const REGISTRATION_OFF = 'Account activation is turned off for now. Try this link again later, or contact your administrator.';
/** The page's own words: as on sign-up, the server's wording never reaches an anonymous reader. */
const PASSWORD_REFUSED = "This password doesn't meet the password policy. Choose a different one.";

const PASSWORD_ID = 'activation-password';
const PASSWORD_REQUIREMENTS_ID = 'activation-password-requirements';
const PASSWORD_ERROR_ID = 'activation-password-error';
const CONFIRM_PASSWORD_ID = 'activation-confirm-password';
const CONFIRM_PASSWORD_ERROR_ID = 'activation-confirm-password-error';

/** Outcomes nothing typed into the form can change, so each replaces it. */
type FinalOutcome = 'active' | 'pending-approval' | 'link-used' | 'link-unusable' | 'registration-off';

function passwordsMatch(password: string, confirmPassword: string): boolean {
    return password.length > 0 && password === confirmPassword;
}

/**
 * Why an activation link cannot be used, or null when it can. Registration creates the account
 * before the email leaves, so a genuine link always names it in its subject.
 *
 * The signature is not checked here -- only the server holds the secret -- and the server reports a
 * forged or expired token as an unexpected failure, so these are the only checks that can tell the
 * reader their link is the problem.
 */
function activationTokenError(token: string, claims: AuthTokenClaims | null): string | null {
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

function describedBy(...ids: (string | false)[]): string {
    return ids.filter(Boolean).join(' ');
}

function ActivationShell({ description, focusTitle, children }: { description?: string; focusTitle?: boolean; children: ReactNode }) {
    return (
        <AuthPageShell
            title="Activate your account"
            description={description}
            focusTitle={focusTitle}
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

/**
 * Where there is nothing left to do but read, the whole page is the message: unboxed text in a
 * status region, and the one control that moves the reader on. Every outcome replaces the form the
 * reader was in, so focus follows it to the title.
 */
function OutcomeShell({ title, message, action }: { title: string; message: string; action: ReactNode }) {
    return (
        <AuthPageShell title={title} focusTitle>
            <div role="status" className="text-sm text-muted-foreground">
                <p>{message}</p>
            </div>
            {action}
        </AuthPageShell>
    );
}

export function ActivationPage() {
    const { token = '' } = useParams<{ token: string }>();
    const tokenClaims = useMemo(() => parseAuthToken(token), [token]);
    const tokenError = useMemo(() => activationTokenError(token, tokenClaims), [token, tokenClaims]);

    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [passwordError, setPasswordError] = useState('');
    const [formError, setFormError] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const [outcome, setOutcome] = useState<FinalOutcome | null>(null);
    const [expiredDuringSession, setExpiredDuringSession] = useState(false);
    const { policy: passwordPolicy, loading: passwordPolicyLoading, error: passwordPolicyError } = usePasswordPolicy();

    const accountName = [tokenClaims?.firstname, tokenClaims?.lastname].filter(Boolean).join(' ');
    const email = tokenClaims?.email ?? '';
    const passwordMismatch = confirmPassword.length > 0 && password !== confirmPassword;
    // Only an outright refusal blocks: 'undecidable' means this browser cannot read the pattern as the
    // server does, and blocking on it would refuse a password the server would accept.
    const policyRefusesPassword = assessPassword(password, passwordPolicy) === 'unsatisfied';
    const canSubmit =
        !tokenError &&
        passwordsMatch(password, confirmPassword) &&
        !policyRefusesPassword &&
        !passwordPolicyLoading &&
        !passwordPolicyError &&
        !submitting;

    async function handleSubmit(event: SubmitEvent) {
        event.preventDefault();
        if (!canSubmit) {
            return;
        }
        // The token was read when the page loaded. A tab left open past its expiry would otherwise send
        // a request the server can only refuse in words written for developers.
        if (tokenClaims && isAuthTokenExpired(tokenClaims)) {
            setExpiredDuringSession(true);
            return;
        }

        setPasswordError('');
        setFormError('');
        setSubmitting(true);
        try {
            const result = await finalizeRegistration({
                token,
                password,
                // Sent only because `RegisterUserEntity` marks both @NotNull and the endpoint is @Valid:
                // omitting them is a 400. Finalize reads them only for a token without a subject, which
                // this page refuses before it gets here.
                firstname: tokenClaims?.firstname ?? '',
                lastname: tokenClaims?.lastname ?? '',
            });

            switch (result.outcome) {
                case 'password-rejected':
                    setPasswordError(PASSWORD_REFUSED);
                    break;
                case 'rejected':
                    // Finalize reports a token that expired in flight as a server failure; only the
                    // claims can tell the two apart.
                    if (tokenClaims && isAuthTokenExpired(tokenClaims)) {
                        setExpiredDuringSession(true);
                    } else {
                        setFormError(result.message);
                    }
                    break;
                default:
                    setOutcome(result.outcome);
            }
        } finally {
            setSubmitting(false);
        }
    }

    const deadLink = tokenError ?? (expiredDuringSession ? EXPIRED_LINK : null) ?? (outcome === 'link-unusable' ? UNUSABLE_LINK : null);

    // A dead link replaces the form entirely: a form left behind an alert invites a password the
    // server is certain to refuse.
    if (deadLink) {
        return (
            // Focus follows only a state that replaced a form in use. A link dead on arrival is simply
            // where the page starts, and the reader is already at the top of it.
            <ActivationShell focusTitle={!tokenError}>
                <Alert variant="destructive" role="alert">
                    <AlertTitle>Could not activate your account</AlertTitle>
                    <AlertDescription>{deadLink}</AlertDescription>
                </Alert>
            </ActivationShell>
        );
    }

    if (outcome === 'registration-off') {
        // Not destructive: nothing is wrong with the link, and it works again once the setting is back.
        return (
            <ActivationShell focusTitle>
                <Alert role="alert">
                    <AlertTitle>Activation is turned off</AlertTitle>
                    <AlertDescription>{REGISTRATION_OFF}</AlertDescription>
                </Alert>
            </ActivationShell>
        );
    }

    if (outcome === 'active' || outcome === 'link-used') {
        return (
            <OutcomeShell
                title={outcome === 'active' ? 'Account activated' : 'Account already activated'}
                message={
                    outcome === 'active'
                        ? 'Your account is ready. Sign in with your email and the password you just chose.'
                        : 'This link has already been used to set a password. Sign in with that password.'
                }
                action={
                    <Button asChild size="lg" className="mt-6 w-full">
                        <Link to="/login">Sign in</Link>
                    </Button>
                }
            />
        );
    }

    if (outcome === 'pending-approval') {
        // No Sign in here: it is the one control guaranteed to fail until an administrator acts.
        return (
            <OutcomeShell
                title="Waiting for approval"
                message="An administrator needs to approve your account before you can sign in. You'll get an email when they do."
                action={
                    <Button asChild variant="outline" size="lg" className="mt-6 w-full">
                        <Link to="/login">Back to sign in</Link>
                    </Button>
                }
            />
        );
    }

    return (
        <ActivationShell description="Choose a password to finish setting up your account.">
            <form onSubmit={handleSubmit} className="space-y-4" noValidate>
                {passwordPolicyError ? (
                    <Alert variant="destructive" role="alert">
                        <AlertTitle>{"Couldn't load password rules"}</AlertTitle>
                        <AlertDescription>{passwordPolicyError}</AlertDescription>
                    </Alert>
                ) : null}

                {formError ? (
                    <Alert variant="destructive" role="alert">
                        <AlertTitle>Could not activate your account</AlertTitle>
                        <AlertDescription>{formError}</AlertDescription>
                    </Alert>
                ) : null}

                {/* The account is context, not input: disabled fields fail contrast, keyboard
                    navigation skips them, and assistive technology never reaches the name they carry. */}
                {accountName || email ? (
                    <p className="text-sm text-muted-foreground">
                        Signing up as <span className="font-medium text-foreground">{accountName || email}</span>
                        {accountName && email ? ` · ${email}` : null}
                    </p>
                ) : null}

                {/* A password manager files a new password under the username beside it, and this form
                    has no visible one to offer. */}
                <input type="email" autoComplete="username" value={email} readOnly hidden />

                <Field orientation="vertical" className="gap-2" data-invalid={passwordError ? true : undefined}>
                    <FieldLabel htmlFor={PASSWORD_ID}>Password</FieldLabel>
                    <PasswordInput
                        id={PASSWORD_ID}
                        value={password}
                        onChange={event => {
                            setPassword(event.target.value);
                            setPasswordError('');
                        }}
                        required
                        autoComplete="new-password"
                        aria-invalid={passwordError ? true : undefined}
                        aria-describedby={describedBy(Boolean(passwordError) && PASSWORD_ERROR_ID, PASSWORD_REQUIREMENTS_ID)}
                        // eslint-disable-next-line jsx-a11y/no-autofocus
                        autoFocus
                    />
                    <FieldError id={PASSWORD_ERROR_ID}>{passwordError || undefined}</FieldError>
                    <div id={PASSWORD_REQUIREMENTS_ID}>
                        <PasswordRequirements policy={passwordPolicy} password={password} showStrengthMeter />
                    </div>
                </Field>

                <Field orientation="vertical" className="gap-2" data-invalid={passwordMismatch ? true : undefined}>
                    <FieldLabel htmlFor={CONFIRM_PASSWORD_ID}>Confirm password</FieldLabel>
                    <PasswordInput
                        id={CONFIRM_PASSWORD_ID}
                        value={confirmPassword}
                        onChange={event => setConfirmPassword(event.target.value)}
                        required
                        autoComplete="new-password"
                        aria-invalid={passwordMismatch ? true : undefined}
                        aria-describedby={describedBy(passwordMismatch && CONFIRM_PASSWORD_ERROR_ID) || undefined}
                    />
                    <FieldError id={CONFIRM_PASSWORD_ERROR_ID}>{passwordMismatch ? 'Both passwords must match.' : undefined}</FieldError>
                </Field>

                <Button type="submit" className="w-full" size="lg" disabled={!canSubmit}>
                    {submitting ? (
                        <span className="inline-flex items-center justify-center gap-2">
                            <Spinner className="size-4 shrink-0" aria-hidden />
                            Activating account…
                        </span>
                    ) : (
                        'Activate account'
                    )}
                </Button>
            </form>
        </ActivationShell>
    );
}
