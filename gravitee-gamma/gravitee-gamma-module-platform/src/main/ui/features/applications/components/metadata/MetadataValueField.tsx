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
import { Input, Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@gravitee/graphene-core';

import type { ApplicationMetadataFormat } from '../../types/applicationNotification';
import { MAIL_PATTERN, URL_PATTERN } from '../../utils/metadataValidators';

export function MetadataValueField({
    id,
    format,
    value,
    disabled,
    onChange,
}: Readonly<{
    id: string;
    format: ApplicationMetadataFormat;
    value: string;
    disabled: boolean;
    onChange: (value: string) => void;
}>) {
    if (format === 'BOOLEAN') {
        return (
            <Select value={value} onValueChange={onChange} disabled={disabled} required>
                <SelectTrigger id={id} className="w-full" aria-required="true">
                    <SelectValue placeholder="Select a value" />
                </SelectTrigger>
                <SelectContent>
                    <SelectItem value="true">true</SelectItem>
                    <SelectItem value="false">false</SelectItem>
                </SelectContent>
            </Select>
        );
    }

    const commonProps = {
        id,
        value,
        onChange: (event: React.ChangeEvent<HTMLInputElement>) => onChange(event.target.value),
        disabled,
        required: true,
        'aria-required': true,
    };

    if (format === 'NUMERIC') {
        return <Input {...commonProps} type="number" placeholder="123000.92" />;
    }
    if (format === 'DATE') {
        return <Input {...commonProps} type="date" />;
    }
    if (format === 'MAIL') {
        return <Input {...commonProps} type="email" pattern={MAIL_PATTERN.source} placeholder="john@doe.com" />;
    }
    if (format === 'URL') {
        return <Input {...commonProps} type="url" pattern={URL_PATTERN.source} placeholder="https://gravitee.io" />;
    }
    return <Input {...commonProps} type="text" placeholder="Operations" />;
}
