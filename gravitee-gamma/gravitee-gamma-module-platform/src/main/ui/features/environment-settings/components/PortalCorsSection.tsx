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

import { Card, CardContent, FieldDescription } from '@gravitee/graphene-core';

import { CorsFields } from '../../organization-settings/components/CorsFields';
import type { CorsFieldReadonly, CorsFormState } from '../../organization-settings/components/CorsSection';

const PORTAL_ORIGIN_DESCRIPTION = 'Exact origins, * , or a regular expression. Press Enter to add.';
const PORTAL_ALLOW_HEADERS_DESCRIPTION = 'Start typing to pick a common header, or press Enter to add your own.';
const PORTAL_EXPOSED_HEADERS_DESCRIPTION = 'Headers the browser is allowed to read from the response.';
const PORTAL_MAX_AGE_DESCRIPTION = 'How long the response from a pre-flight request can be cached by clients';

export function PortalCorsSection({
    value,
    disabled,
    readonly = {},
    onChange,
}: Readonly<{
    value: CorsFormState;
    disabled: boolean;
    readonly?: CorsFieldReadonly;
    onChange: (next: CorsFormState) => void;
}>) {
    return (
        <Card>
            <CardContent className="space-y-4 pt-6">
                <CorsFields
                    value={value}
                    disabled={disabled}
                    readonly={readonly}
                    onChange={onChange}
                    allowOriginId="cors-origin"
                    allowOriginAddOnBlur={false}
                    allowOriginTrail={<FieldDescription>{PORTAL_ORIGIN_DESCRIPTION}</FieldDescription>}
                    allowHeadersDescription={PORTAL_ALLOW_HEADERS_DESCRIPTION}
                    exposedHeadersDescription={PORTAL_EXPOSED_HEADERS_DESCRIPTION}
                    maxAgeLabel="Max age (seconds)"
                    maxAgeDescription={PORTAL_MAX_AGE_DESCRIPTION}
                />
            </CardContent>
        </Card>
    );
}
