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
import { Field, FieldDescription, FieldLabel, Tabs, TabsContent, TabsList, TabsTrigger } from '@gravitee/graphene-core';
import type { ReactNode } from 'react';

import { CREDENTIAL_STRATEGIES, type CredentialStrategy } from '../types/credential';
import { credentialStrategyLabel, isKnownCredentialStrategy } from '../utils/credentialStrategies';

/** The type as tabs; `children` are the fields of the selected type. */
export function CredentialTypeField({
    value,
    onChange,
    children,
}: Readonly<{ value: CredentialStrategy; onChange: (strategy: CredentialStrategy) => void; children: ReactNode }>) {
    return (
        <Tabs value={value} onValueChange={next => isKnownCredentialStrategy(next) && onChange(next)} className="gap-4">
            <Field orientation="vertical" className="gap-1.5">
                <FieldLabel id="credential-type-label" required>
                    Type
                </FieldLabel>
                <TabsList aria-labelledby="credential-type-label" aria-describedby="credential-type-description">
                    {CREDENTIAL_STRATEGIES.map(strategy => (
                        <TabsTrigger key={strategy} value={strategy}>
                            {credentialStrategyLabel(strategy)}
                        </TabsTrigger>
                    ))}
                </TabsList>
                <FieldDescription id="credential-type-description">The type cannot change once the credential exists.</FieldDescription>
            </Field>
            {CREDENTIAL_STRATEGIES.map(strategy => (
                <TabsContent key={strategy} value={strategy} className="space-y-4">
                    {children}
                </TabsContent>
            ))}
        </Tabs>
    );
}
