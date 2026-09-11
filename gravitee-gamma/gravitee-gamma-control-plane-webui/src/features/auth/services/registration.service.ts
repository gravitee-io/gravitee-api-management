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
