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
export const INTEGRATION_NAME_MAX = 50;
export const INTEGRATION_DESCRIPTION_MAX = 250;

export interface IntegrationFormValues {
    name: string;
    description: string;
}

export interface IntegrationFormErrors {
    name?: string;
    description?: string;
}

function validateName(name: string): string | undefined {
    if (name === '') {
        return 'Name is required.';
    }
    if (name.length > INTEGRATION_NAME_MAX) {
        return `Name can not exceed ${INTEGRATION_NAME_MAX} characters.`;
    }
    return undefined;
}

function validateDescription(description: string): string | undefined {
    if (description.length > INTEGRATION_DESCRIPTION_MAX) {
        return `Description can not exceed ${INTEGRATION_DESCRIPTION_MAX} characters.`;
    }
    return undefined;
}

export function validateIntegrationForm({ name, description }: IntegrationFormValues): IntegrationFormErrors {
    const errors: IntegrationFormErrors = {};
    const nameError = validateName(name);
    if (nameError) {
        errors.name = nameError;
    }
    const descriptionError = validateDescription(description);
    if (descriptionError) {
        errors.description = descriptionError;
    }
    return errors;
}
