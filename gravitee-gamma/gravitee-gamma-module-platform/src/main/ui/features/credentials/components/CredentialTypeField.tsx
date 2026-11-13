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
import { Field, FieldDescription, FieldLabel, ToggleGroup, ToggleGroupItem } from '@gravitee/graphene-core';

import { CREDENTIAL_STRATEGIES, type CredentialStrategy } from '../types/credential';
import { credentialStrategyLabel, isKnownCredentialStrategy } from '../utils/credentialStrategies';

export function CredentialTypeField({
    value,
    onChange,
}: Readonly<{ value: CredentialStrategy; onChange: (strategy: CredentialStrategy) => void }>) {
    return (
        <Field orientation="vertical" className="gap-1.5">
            <FieldLabel id="credential-type-label" required>
                Type
            </FieldLabel>
            <ToggleGroup
                type="single"
                className="flex-wrap"
                value={value}
                onValueChange={next => isKnownCredentialStrategy(next) && onChange(next)}
                aria-labelledby="credential-type-label"
                aria-describedby="credential-type-description"
            >
                {CREDENTIAL_STRATEGIES.map(strategy => (
                    <ToggleGroupItem key={strategy} value={strategy}>
                        {credentialStrategyLabel(strategy)}
                    </ToggleGroupItem>
                ))}
            </ToggleGroup>
            <FieldDescription id="credential-type-description">The type cannot change once the credential exists.</FieldDescription>
        </Field>
    );
}
