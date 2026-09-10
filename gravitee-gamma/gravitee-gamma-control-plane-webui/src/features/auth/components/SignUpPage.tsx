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
    FieldDescription,
    FieldError,
    FieldGroup,
    FieldLabel,
    FieldLegend,
    FieldSet,
    Input,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Spinner,
} from '@gravitee/graphene-core';
import { useEffect, useState, type SubmitEvent } from 'react';
import { Link } from 'react-router-dom';

import { AuthPageShell } from './AuthPageShell';
import { useBootstrapStore } from '../../../shared/config/bootstrap.store';
import { fetchCustomUserFields, submitRegistration, type CustomUserField } from '../services/registration.service';

/**
 * The server's `EmailValidator` (OWASP pattern and length cap), copied rather than approximated: a
 * looser check lets through addresses the server then rejects, a stricter one blocks addresses it
 * would accept. Keep the two in step.
 */
const EMAIL_PATTERN = /^[a-zA-Z0-9_+&*-]+(?:\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}$/;
const EMAIL_MAX_LENGTH = 320;

const INVALID_EMAIL = 'Enter a valid email address.';

function fieldId(key: string): string {
    return `sign-up-field-${key}`;
}

function isAnswered(value: string | undefined): boolean {
    return Boolean(value?.trim());
}

/**
 * Answers worth sending. An untouched optional field would otherwise be stored as an empty piece
 * of user metadata, which reads to an administrator as an answer that was given and left blank.
 */
function answeredFields(answers: Record<string, string>): Record<string, string> {
    return Object.fromEntries(Object.entries(answers).filter(([, value]) => isAnswered(value)));
}

export function SignUpPage() {
    const [customUserFields, setCustomUserFields] = useState<CustomUserField[]>([]);
    const [customUserFieldsLoading, setCustomUserFieldsLoading] = useState(true);
    const [customUserFieldsFailed, setCustomUserFieldsFailed] = useState(false);

    const [firstname, setFirstname] = useState('');
    const [lastname, setLastname] = useState('');
    const [email, setEmail] = useState('');
    const [emailTouched, setEmailTouched] = useState(false);
    const [answers, setAnswers] = useState<Record<string, string>>({});

    const [emailRejected, setEmailRejected] = useState(false);
    const [formError, setFormError] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const [sentTo, setSentTo] = useState<string | null>(null);
    const automaticValidationEnabled = useBootstrapStore(s => s.config?.automaticValidationEnabled ?? false);

    useEffect(() => {
        let active = true;
        fetchCustomUserFields()
            .then(fields => {
                if (active) {
                    setCustomUserFields(fields);
                }
            })
            .catch(() => {
                if (active) {
                    setCustomUserFieldsFailed(true);
                }
            })
            .finally(() => {
                if (active) {
                    setCustomUserFieldsLoading(false);
                }
            });
        return () => {
            active = false;
        };
    }, []);

    const trimmedEmail = email.trim();
    const emailValid = trimmedEmail.length <= EMAIL_MAX_LENGTH && EMAIL_PATTERN.test(trimmedEmail);
    // The shape check waits for the field to be left: complaining at the first character tells
    // everyone their address is wrong while they are still typing it.
    const emailError = emailRejected || (emailTouched && email.length > 0 && !emailValid) ? INVALID_EMAIL : '';
    const requiredFieldsAnswered = customUserFields.every(field => !field.required || isAnswered(answers[field.key]));
    // The read has to finish first: submitting before it does would silently skip fields the
    // administrator made mandatory. A read that failed leaves nothing to wait for.
    const canSubmit =
        isAnswered(firstname) && isAnswered(lastname) && emailValid && requiredFieldsAnswered && !customUserFieldsLoading && !submitting;

    function answer(key: string, value: string) {
        setAnswers(current => ({ ...current, [key]: value }));
    }

    async function handleSubmit(event: SubmitEvent) {
        event.preventDefault();
        if (!canSubmit) {
            return;
        }

        setEmailRejected(false);
        setFormError('');
        setSubmitting(true);
        const submittedEmail = email.trim();
        try {
            const result = await submitRegistration({
                firstname: firstname.trim(),
                lastname: lastname.trim(),
                email: submittedEmail,
                ...(customUserFields.length > 0 ? { customFields: answeredFields(answers) } : {}),
            });

            switch (result.outcome) {
                case 'submitted':
                    setSentTo(submittedEmail);
                    break;
                case 'email-rejected':
                    setEmailRejected(true);
                    break;
                case 'rejected':
                    setFormError(result.message);
                    break;
            }
        } finally {
            setSubmitting(false);
        }
    }

    if (sentTo) {
        return (
            <AuthPageShell title="Check your email">
                {/* No resend control: the management API has no endpoint that could serve one, and a
                    button that cannot work is worse than its absence. */}
                {/* Unboxed: the whole page is the message, so an Alert here only framed the card's
                    content a second time. */}
                <div role="status" className="space-y-3 text-sm text-muted-foreground">
                    {/* The address is echoed back so a typo is caught here, where the form can
                        still be filled in again, rather than in an inbox nothing ever reaches. */}
                    <p>
                        Your request has been sent. The activation link goes to{' '}
                        <span className="font-medium text-foreground">{sentTo}</span>.
                    </p>
                    {/* With automatic validation off the server emails nothing until an
                        administrator approves, so the page must not promise an email yet. */}
                    <p>
                        {automaticValidationEnabled
                            ? 'If it has not arrived within a few minutes, check your spam folder.'
                            : 'An administrator reviews each request first. The link is sent once yours is approved.'}
                    </p>
                </div>
                {/* The only thing left to do here, so it is the page's control rather than muted
                    footer text -- and outline, because Solaris orange is for primary actions. */}
                <Button asChild variant="outline" size="lg" className="mt-6 w-full">
                    <Link to="/login">Back to sign in</Link>
                </Button>
            </AuthPageShell>
        );
    }

    return (
        <AuthPageShell
            title="Request an account"
            description="We'll email you a link to activate your account."
            footer={
                <>
                    Already have an account?{' '}
                    <Link to="/login" className="text-primary underline-offset-4 hover:underline">
                        Sign in
                    </Link>
                </>
            }
        >
            <form onSubmit={handleSubmit} className="space-y-6" noValidate>
                {customUserFieldsFailed ? (
                    <Alert role="alert">
                        <AlertTitle>{"Couldn't load the additional questions"}</AlertTitle>
                        <AlertDescription>
                            You can still request an account. Your administrator may ask for the rest later.
                        </AlertDescription>
                    </Alert>
                ) : null}

                {formError ? (
                    <Alert variant="destructive" role="alert">
                        <AlertTitle>Could not send your request</AlertTitle>
                        <AlertDescription>{formError}</AlertDescription>
                    </Alert>
                ) : null}

                <FieldSet>
                    <FieldLegend>Your details</FieldLegend>
                    <FieldGroup>
                        <Field orientation="vertical" className="gap-2">
                            <FieldLabel htmlFor="sign-up-firstname" required>
                                First name
                            </FieldLabel>
                            <Input
                                id="sign-up-firstname"
                                value={firstname}
                                onChange={event => setFirstname(event.target.value)}
                                required
                                autoComplete="given-name"
                                // eslint-disable-next-line jsx-a11y/no-autofocus
                                autoFocus
                            />
                        </Field>

                        <Field orientation="vertical" className="gap-2">
                            <FieldLabel htmlFor="sign-up-lastname" required>
                                Last name
                            </FieldLabel>
                            <Input
                                id="sign-up-lastname"
                                value={lastname}
                                onChange={event => setLastname(event.target.value)}
                                required
                                autoComplete="family-name"
                            />
                        </Field>

                        <Field orientation="vertical" className="gap-2" data-invalid={emailError ? true : undefined}>
                            <FieldLabel htmlFor="sign-up-email" required>
                                Email
                            </FieldLabel>
                            <Input
                                id="sign-up-email"
                                type="email"
                                value={email}
                                onChange={event => {
                                    // A rejection describes the address that was sent, so editing it retires it.
                                    setEmail(event.target.value);
                                    setEmailRejected(false);
                                }}
                                onBlur={() => setEmailTouched(true)}
                                required
                                aria-invalid={emailError ? true : undefined}
                                autoComplete="email"
                            />
                            <FieldError>{emailError || undefined}</FieldError>
                        </Field>
                    </FieldGroup>
                </FieldSet>

                {customUserFields.length > 0 ? (
                    <FieldSet>
                        <FieldLegend>Additional details</FieldLegend>
                        {/* Names who is asking, so the extra questions read as a request from the
                            organization rather than as an interrogation by the product. */}
                        <FieldDescription>Your administrator asks everyone requesting an account for these.</FieldDescription>
                        <FieldGroup>
                            {customUserFields.map(field => (
                                <CustomUserFieldControl
                                    key={field.key}
                                    field={field}
                                    value={answers[field.key] ?? ''}
                                    onChange={value => answer(field.key, value)}
                                />
                            ))}
                        </FieldGroup>
                    </FieldSet>
                ) : null}

                <Button type="submit" className="w-full" size="lg" disabled={!canSubmit}>
                    {submitting ? (
                        <span className="inline-flex items-center justify-center gap-2">
                            <Spinner className="size-4 shrink-0" aria-hidden />
                            Sending request…
                        </span>
                    ) : (
                        'Request account'
                    )}
                </Button>
            </form>
        </AuthPageShell>
    );
}

/**
 * One administrator-configured field. A fixed list of values is a choice and renders as one;
 * anything else is free text, matching what the classic console offers for the same configuration.
 */
function CustomUserFieldControl({ field, value, onChange }: { field: CustomUserField; value: string; onChange: (value: string) => void }) {
    const id = fieldId(field.key);
    const hasChoices = (field.values?.length ?? 0) > 0;

    return (
        <Field orientation="vertical" className="gap-2">
            <FieldLabel htmlFor={id} required={field.required}>
                {field.label}
            </FieldLabel>
            {hasChoices ? (
                <Select value={value || undefined} onValueChange={onChange} required={field.required}>
                    <SelectTrigger id={id} className="w-full">
                        <SelectValue placeholder={`Select ${field.label.toLowerCase()}`} />
                    </SelectTrigger>
                    <SelectContent>
                        {field.values?.map(option => (
                            <SelectItem key={option} value={option}>
                                {option}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
            ) : (
                <Input id={id} value={value} onChange={event => onChange(event.target.value)} required={field.required} />
            )}
        </Field>
    );
}
