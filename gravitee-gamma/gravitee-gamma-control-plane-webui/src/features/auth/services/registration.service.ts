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
import { getReCaptchaHeaderName, resolveReCaptchaToken } from './recaptcha.service';
import { ApiError, managementApi } from '../../../shared/api/api-client';

/** One field an administrator added to the sign-up form (`CustomUserFieldEntity`). */
export interface CustomUserField {
    readonly key: string;
    readonly label: string;
    /** Absent or empty for a free-text field; a fixed list makes the field a choice. */
    readonly values?: readonly string[];
    readonly required: boolean;
}

export interface RegistrationRequest {
    readonly firstname: string;
    readonly lastname: string;
    readonly email: string;
    readonly customFields?: Readonly<Record<string, string>>;
}

export type RegistrationResult =
    /** Nothing more is owed to the caller: an activation email is on its way, or deliberately is not. */
    | { readonly outcome: 'submitted' }
    /** The server refused the address itself. */
    | { readonly outcome: 'email-rejected' }
    /** Refused for a reason no single input owns, worded for the reader. */
    | { readonly outcome: 'rejected'; readonly message: string };

/**
 * Sends the activation link to the Gamma registration page rather than the classic console.
 * Only this fixed keyword is accepted -- `UsersRegistrationResource` rejects a URL.
 */
const GAMMA_REGISTRATION_TARGET = 'gamma';

const UNEXPECTED_FAILURE = 'Something went wrong while sending your request. Try again.';
const REGISTRATION_OFF = 'Sign-up is turned off for this organization. Ask your administrator for an account.';

const EMAIL_REJECTED_CODE = 'email.invalid';
const REGISTRATION_DISABLED_CODE = 'user.registration.disabled';

/**
 * Whether a rejection tells the caller only that the address is already registered.
 *
 * `POST /users/registration` reaches `InvalidUserException.cannotBeCreated` on exactly one
 * condition -- `userRepository.findBySource` already holds the address -- and its sibling
 * `user.invalid` throw is behind a service-account branch registration never takes. Passing that
 * through would give an anonymous caller an account-existence oracle in plain language, so the
 * page reports the same outcome either way (FOUND-303). The server-side fix is that story's.
 */
function isAccountExistenceOracle(error: ApiError): boolean {
    return error.technicalCode === 'user.invalid';
}

/**
 * The server's text is never shown: the caller is anonymous, and what arrives is an exception's own
 * message -- at worst naming the organization, as a missing Gamma URL does in a 400.
 */
function toResult(error: ApiError): RegistrationResult {
    if (isAccountExistenceOracle(error)) {
        return { outcome: 'submitted' };
    }
    if (error.technicalCode === EMAIL_REJECTED_CODE) {
        return { outcome: 'email-rejected' };
    }
    if (error.technicalCode === REGISTRATION_DISABLED_CODE) {
        return { outcome: 'rejected', message: REGISTRATION_OFF };
    }
    return { outcome: 'rejected', message: UNEXPECTED_FAILURE };
}

/** The fields an administrator added to the sign-up form. Anonymous; may legitimately be empty. */
export async function fetchCustomUserFields(): Promise<CustomUserField[]> {
    return managementApi.get<CustomUserField[]>('/configuration/custom-user-fields');
}

/**
 * Requests an account. Every failure comes back as a {@link RegistrationResult} so the page can
 * render it against the input that caused it -- the alternative is a toast the reader may miss.
 */
export async function submitRegistration(request: RegistrationRequest): Promise<RegistrationResult> {
    try {
        const reCaptchaToken = await resolveReCaptchaToken('register');
        const extraHeaders: Record<string, string> = {};
        if (reCaptchaToken) {
            extraHeaders[getReCaptchaHeaderName()] = reCaptchaToken;
        }

        await managementApi.post<void>(
            `/users/registration?registrationTarget=${GAMMA_REGISTRATION_TARGET}`,
            {
                firstname: request.firstname,
                lastname: request.lastname,
                email: request.email,
                ...(request.customFields ? { customFields: request.customFields } : {}),
            },
            extraHeaders,
        );
        return { outcome: 'submitted' };
    } catch (error) {
        if (error instanceof ApiError) {
            return toResult(error);
        }
        if (process.env.NODE_ENV !== 'production') {
            console.error('Registration request failed', error);
        }
        return { outcome: 'rejected', message: UNEXPECTED_FAILURE };
    }
}

export interface ActivationRequest {
    readonly token: string;
    readonly password: string;
    readonly firstname: string;
    readonly lastname: string;
}

export type ActivationResult =
    /** The account can be signed in to now. */
    | { readonly outcome: 'active' }
    /** The account exists but an administrator has to accept it first; an email follows. */
    | { readonly outcome: 'pending-approval' }
    /** The password policy refused the password; nothing else about the request was wrong. */
    | { readonly outcome: 'password-rejected' }
    /** The link already set a password. */
    | { readonly outcome: 'link-used' }
    /** The link cannot complete an activation, and resubmitting will not change that. */
    | { readonly outcome: 'link-unusable' }
    /** Registration is switched off for the organization. The link works again once it is back on. */
    | { readonly outcome: 'registration-off' }
    /** Failed for a reason no retry is ruled out for. */
    | { readonly outcome: 'rejected'; readonly message: string };

const UNEXPECTED_ACTIVATION_FAILURE = 'Something went wrong while activating your account. Try again.';

/**
 * A defensive branch, not a common one. Registration emails a Gamma link only when automatic
 * validation is on, and then the account it names is created ACTIVE. With validation off, the link
 * that follows an administrator's approval goes to the portal or the classic console instead
 * (`processRegistration`). This code reaches the page only if an administrator set the account back
 * to PENDING after the email left; finalize refuses it before saving the password.
 */
const PENDING_APPROVAL_CODE = 'user.registration.pendingApproval';
const ALREADY_FINALIZED_CODE = 'user.finalized';
/** Deleted, rejected or archived since the email was sent. */
const LINK_UNUSABLE_CODES = new Set(['user.notFound', 'user.state.conflict']);

/**
 * As for sign-up, the server's text is never shown: the caller is anonymous, and what arrives is an
 * exception's own message -- for an expired or forged token, the JWT library's wording in a 500.
 * The page words every outcome itself.
 */
function toActivationResult(error: ApiError): ActivationResult {
    const code = error.technicalCode;
    if (code === 'passwordFormat.invalid') {
        return { outcome: 'password-rejected' };
    }
    if (code === PENDING_APPROVAL_CODE) {
        return { outcome: 'pending-approval' };
    }
    if (code === ALREADY_FINALIZED_CODE) {
        return { outcome: 'link-used' };
    }
    if (code === REGISTRATION_DISABLED_CODE) {
        return { outcome: 'registration-off' };
    }
    if (code && LINK_UNUSABLE_CODES.has(code)) {
        return { outcome: 'link-unusable' };
    }
    return { outcome: 'rejected', message: UNEXPECTED_ACTIVATION_FAILURE };
}

/**
 * Sets the password on the account an activation email was sent for. The returned status decides the
 * outcome rather than the automatic-validation setting, which can change between sign-up and here.
 */
export async function finalizeRegistration(request: ActivationRequest): Promise<ActivationResult> {
    try {
        const reCaptchaToken = await resolveReCaptchaToken('finalizeRegistration');
        const extraHeaders: Record<string, string> = {};
        if (reCaptchaToken) {
            extraHeaders[getReCaptchaHeaderName()] = reCaptchaToken;
        }

        const user = await managementApi.post<{ status?: string }>(
            '/users/registration/finalize',
            {
                token: request.token,
                password: request.password,
                firstname: request.firstname,
                lastname: request.lastname,
            },
            extraHeaders,
        );
        // A 200 means the password was saved: finalize refuses PENDING, REJECTED and ARCHIVED accounts
        // before it writes anything. Only an explicit PENDING is treated as waiting, so a response that
        // omits the status cannot tell someone whose account is ready to wait for an approval that
        // never comes.
        return user?.status === 'PENDING' ? { outcome: 'pending-approval' } : { outcome: 'active' };
    } catch (error) {
        if (error instanceof ApiError) {
            return toActivationResult(error);
        }
        if (process.env.NODE_ENV !== 'production') {
            console.error('Activation request failed', error);
        }
        return { outcome: 'rejected', message: UNEXPECTED_ACTIVATION_FAILURE };
    }
}
