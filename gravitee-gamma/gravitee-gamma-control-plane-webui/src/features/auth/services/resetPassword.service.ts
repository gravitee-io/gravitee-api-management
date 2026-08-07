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

export interface FinalizeResetPasswordPayload {
    token: string;
    password: string;
    firstname: string;
    lastname: string;
}

const RESET_PASSWORD_FALLBACK_ERROR = 'An error occurred while resetting your password.';

export async function finalizeResetPassword(userId: string, payload: FinalizeResetPasswordPayload): Promise<void> {
    const reCaptchaToken = await resolveReCaptchaToken('register');
    const extraHeaders: Record<string, string> = {};
    if (reCaptchaToken) {
        extraHeaders[getReCaptchaHeaderName()] = reCaptchaToken;
    }

    try {
        await managementApi.post<void>(
            `/users/${encodeURIComponent(userId)}/changePassword`,
            {
                token: payload.token,
                password: payload.password,
                firstname: payload.firstname,
                lastname: payload.lastname,
            },
            extraHeaders,
        );
    } catch (error) {
        if (error instanceof ApiError) {
            throw new Error(error.message || RESET_PASSWORD_FALLBACK_ERROR);
        }
        throw error;
    }
}
