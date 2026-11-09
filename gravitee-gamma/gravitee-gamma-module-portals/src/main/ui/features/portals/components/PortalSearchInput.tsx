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

import { InputGroup, InputGroupAddon, InputGroupInput } from '@gravitee/graphene-core';
import { SearchIcon } from '@gravitee/graphene-core/icons';

interface PortalSearchInputProps {
    readonly value: string;
    readonly onChange: (value: string) => void;
}

export function PortalSearchInput({ value, onChange }: PortalSearchInputProps) {
    return (
        <InputGroup className="w-full sm:w-64">
            <InputGroupAddon>
                <SearchIcon aria-hidden="true" />
            </InputGroupAddon>
            <InputGroupInput
                aria-label="Search portals"
                placeholder="Search portals"
                value={value}
                onChange={event => onChange(event.target.value)}
            />
        </InputGroup>
    );
}
