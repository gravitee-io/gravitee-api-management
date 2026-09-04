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

import { Alert, AlertDescription, Card, CardContent, FieldDescription } from '@gravitee/graphene-core';
import { InfoIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import { CorsFields } from './CorsFields';
import type { CorsFieldReadonly, CorsFormState } from './CorsSection';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';

export function ManagementCorsSection({
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
    const [pendingWildcardOrigins, setPendingWildcardOrigins] = useState<string[] | null>(null);
    function handleOriginsChange(next: string[]) {
        if (next.includes('*') && !value.allowOrigin.includes('*')) {
            setPendingWildcardOrigins(next);
            return;
        }
        onChange({ ...value, allowOrigin: next });
    }

    return (
        <Card>
            <CardContent className="space-y-4 pt-6">
                <CorsFields
                    value={value}
                    disabled={disabled}
                    readonly={readonly}
                    onChange={onChange}
                    allowOriginId="cors-allow-origin"
                    allowOriginPlaceholder="*, https://mydomain.com, (http|https).*.mydomain.com, ..."
                    allowOriginAddOnBlur
                    onAllowOriginChange={handleOriginsChange}
                    allowOriginLead={
                        <FieldDescription>
                            The origin parameter specifies a URI that may access the resource. Scheme, domain and port are part of the
                            same-origin definition. If you choose to enable * it means that it allows all requests, regardless of origin.
                            Regular expressions are also supported.
                        </FieldDescription>
                    }
                    allowOriginTrail={
                        <>
                            {value.allowOrigin.includes('*') ? (
                                <Alert>
                                    <InfoIcon className="size-4" />
                                    <AlertDescription>
                                        Setting <span className="font-mono">*</span> exposes this management API to any website. Make sure
                                        that is intended.
                                    </AlertDescription>
                                </Alert>
                            ) : null}
                        </>
                    }
                />

                <ConfirmDialog
                    open={pendingWildcardOrigins !== null}
                    onOpenChange={open => !open && setPendingWildcardOrigins(null)}
                    title="Are you sure?"
                    description="Do you want to remove all cross-origin restrictions?"
                    confirmLabel="Yes, I want to allow all origins."
                    onConfirm={() => {
                        if (pendingWildcardOrigins !== null) {
                            onChange({ ...value, allowOrigin: pendingWildcardOrigins });
                        }
                        setPendingWildcardOrigins(null);
                    }}
                />
            </CardContent>
        </Card>
    );
}
