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
import { Label, Textarea } from '@gravitee/graphene-core';

import { ChipInput } from '../../../shared/components';
import type { ApplicationTypeConfig } from '../../types/applicationCreate';
import { AdditionalClientMetadataField } from '../shared/AdditionalClientMetadataField';
import { GrantTypeChips } from '../shared/GrantTypeChips';

interface OAuthSecurityFieldsProps {
    readonly selectedType: ApplicationTypeConfig;
    readonly grantTypes: string[];
    readonly redirectUris: string[];
    readonly clientCertificate: string;
    readonly additionalClientMetadata: Record<string, string> | null;
    readonly onGrantTypesChange: (grantTypes: string[]) => void;
    readonly onRedirectUrisChange: (redirectUris: string[]) => void;
    readonly onClientCertificateChange: (value: string) => void;
    readonly onAdditionalClientMetadataChange: (value: Record<string, string> | null) => void;
    readonly onMetadataDuplicateKeysChange?: (hasDuplicates: boolean) => void;
}

export function OAuthSecurityFields({
    selectedType,
    grantTypes,
    redirectUris,
    clientCertificate,
    additionalClientMetadata,
    onGrantTypesChange,
    onRedirectUrisChange,
    onClientCertificateChange,
    onAdditionalClientMetadataChange,
    onMetadataDuplicateKeysChange,
}: OAuthSecurityFieldsProps) {
    const requiresRedirectUris = Boolean(selectedType.requires_redirect_uris);

    return (
        <div className="space-y-5 pt-2">
            <div className="space-y-2">
                <Label>Allowed grant types</Label>
                <GrantTypeChips typeConfig={selectedType} values={grantTypes} onChange={onGrantTypesChange} />
                <p className="text-xs text-muted-foreground">
                    Grant types allowed for the client. Please set only grant types you need for security reasons.
                </p>
            </div>

            {requiresRedirectUris && (
                <div className="space-y-2">
                    <Label htmlFor="redirect-uris">
                        Redirect URIs <span className="text-destructive">*</span>
                    </Label>
                    <ChipInput
                        id="redirect-uris"
                        values={redirectUris}
                        onChange={onRedirectUrisChange}
                        placeholder="Enter a redirect URI"
                    />
                    <p className="text-xs text-muted-foreground">URIs where the authorization server will send OAuth responses.</p>
                </div>
            )}

            <div className="space-y-2">
                <Label htmlFor="client-cert">Client Certificate (PEM Only)</Label>
                <Textarea
                    id="client-cert"
                    value={clientCertificate}
                    onChange={event => onClientCertificateChange(event.target.value)}
                    placeholder={'-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----'}
                    rows={4}
                    className="min-h-20 font-mono text-xs"
                />
                <p className="text-xs text-muted-foreground">
                    The <code className="rounded bg-muted px-1 py-0.5 text-xs">client_certificate</code> of the application. This field is
                    required to subscribe to certain mTLS plans.
                </p>
            </div>

            <AdditionalClientMetadataField
                key={selectedType.id}
                value={additionalClientMetadata}
                onChange={onAdditionalClientMetadataChange}
                onDuplicateKeysChange={onMetadataDuplicateKeysChange}
            />
        </div>
    );
}
