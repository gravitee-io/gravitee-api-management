/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { isValidEmail } from '../../../shared/utils/email';
import type { UserType } from '../types/user';
import { GRAVITEE_IDP } from '../types/user';

export interface AddUserFormValues {
    type: UserType;
    firstName: string;
    lastName: string;
    email: string;
    source: string;
    sourceId: string;
}

export function isAddUserFormValid(
    form: AddUserFormValues,
    options: { showIdentityProviderFields: boolean; identityProvidersReady: boolean },
): boolean {
    const isServiceAccount = form.type === 'SERVICE_ACCOUNT';
    const lastNameValid = form.lastName.trim().length > 0;

    if (isServiceAccount) {
        const emailValue = form.email.trim();
        const emailValid = emailValue === '' || isValidEmail(emailValue);
        return lastNameValid && emailValid;
    }

    const firstNameValid = form.firstName.trim().length > 0;
    const emailValue = form.email.trim();
    const emailValid = emailValue.length > 0 && isValidEmail(emailValue);
    const sourceIdValid =
        !options.showIdentityProviderFields ||
        !options.identityProvidersReady ||
        form.source === GRAVITEE_IDP.id ||
        form.sourceId.trim().length > 0;

    return lastNameValid && firstNameValid && emailValid && sourceIdValid;
}

export function resolvePreRegisterUserSource(
    isServiceAccount: boolean,
    showIdentityProviderFields: boolean,
    selectedSource: string,
): string {
    if (isServiceAccount || !showIdentityProviderFields) {
        return GRAVITEE_IDP.id;
    }
    return selectedSource;
}

export function applyUserTypeChange(form: AddUserFormValues, nextType: UserType): AddUserFormValues {
    if (nextType === form.type) {
        return form;
    }
    if (nextType === 'SERVICE_ACCOUNT') {
        return {
            ...form,
            type: nextType,
            firstName: '',
            source: GRAVITEE_IDP.id,
            sourceId: '',
        };
    }
    return { ...form, type: nextType };
}
