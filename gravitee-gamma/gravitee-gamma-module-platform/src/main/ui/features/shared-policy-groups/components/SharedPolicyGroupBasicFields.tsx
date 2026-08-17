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

import { Field, FieldDescription, FieldLabel, Input, Textarea } from '@gravitee/graphene-core';

import {
    DESCRIPTION_MAX_LENGTH,
    PREREQUISITE_MESSAGE_MAX_LENGTH,
    PREREQUISITE_MESSAGE_PLACEHOLDER,
    type SharedPolicyGroupBasicFormValues,
} from '../utils/sharedPolicyGroupPayload';

export function SharedPolicyGroupBasicFields({
    idPrefix,
    values,
    disabled,
    onChange,
}: Readonly<{
    idPrefix: string;
    values: SharedPolicyGroupBasicFormValues;
    disabled?: boolean;
    onChange: <K extends keyof SharedPolicyGroupBasicFormValues>(key: K, value: SharedPolicyGroupBasicFormValues[K]) => void;
}>) {
    const nameId = `${idPrefix}-name`;
    const descriptionId = `${idPrefix}-description`;
    const prerequisiteId = `${idPrefix}-prerequisite-message`;

    return (
        <>
            <h3 className="text-sm font-semibold">Basic information</h3>

            <Field orientation="vertical" className="gap-1.5">
                <FieldLabel htmlFor={nameId} required>
                    Name
                </FieldLabel>
                <Input
                    id={nameId}
                    value={values.name}
                    onChange={e => onChange('name', e.target.value)}
                    placeholder="e.g. Default authentication"
                    maxLength={512}
                    disabled={disabled}
                    required
                />
            </Field>

            <Field orientation="vertical" className="gap-1.5">
                <FieldLabel htmlFor={descriptionId}>Describe the purpose of this policy group</FieldLabel>
                <FieldDescription>{DESCRIPTION_MAX_LENGTH} characters max.</FieldDescription>
                <Textarea
                    id={descriptionId}
                    value={values.description}
                    onChange={e => onChange('description', e.target.value)}
                    placeholder="Describe what this policy group is used for"
                    maxLength={DESCRIPTION_MAX_LENGTH}
                    disabled={disabled}
                />
            </Field>

            <Field orientation="vertical" className="gap-1.5">
                <FieldLabel htmlFor={prerequisiteId}>Prerequisite message</FieldLabel>
                <FieldDescription>{PREREQUISITE_MESSAGE_MAX_LENGTH} characters max.</FieldDescription>
                <Textarea
                    id={prerequisiteId}
                    value={values.prerequisiteMessage}
                    onChange={e => onChange('prerequisiteMessage', e.target.value)}
                    placeholder={PREREQUISITE_MESSAGE_PLACEHOLDER}
                    maxLength={PREREQUISITE_MESSAGE_MAX_LENGTH}
                    disabled={disabled}
                />
            </Field>
        </>
    );
}
