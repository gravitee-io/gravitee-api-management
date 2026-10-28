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

import { Checkbox, Field, FieldLabel } from '@gravitee/graphene-core';

export function DictionaryEncryptionField({
    checked,
    onCheckedChange,
    disabled,
}: Readonly<{
    checked: boolean;
    onCheckedChange: (checked: boolean) => void;
    disabled?: boolean;
}>) {
    return (
        <Field orientation="vertical" className="gap-1.5">
            <div className="flex items-center gap-2">
                <Checkbox
                    id="dictionary-encrypt-on-fetch"
                    checked={checked}
                    onCheckedChange={value => onCheckedChange(value === true)}
                    disabled={disabled}
                />
                <FieldLabel htmlFor="dictionary-encrypt-on-fetch">Encrypt fetched values</FieldLabel>
            </div>
            <p className="text-xs text-muted-foreground">
                Existing plaintext values are encrypted the next time they are fetched. Already-encrypted values are left untouched, and
                turning this off does not decrypt anything already stored.
            </p>
        </Field>
    );
}
