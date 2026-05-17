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
import { Badge, cn, Label, Textarea } from '@gravitee/graphene-core';
import { CircleCheckIcon } from '@gravitee/graphene-core/icons';

import { AdditionalClientMetadataField } from './AdditionalClientMetadataField';
import { ChipInput } from '../../../shared/components';
import type { ApplicationTypeConfig } from '../../types/applicationCreate';
import { isMandatoryGrantType } from '../../utils/applicationCreateMapper';

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
}: OAuthSecurityFieldsProps) {
    const requiresRedirectUris = Boolean(selectedType.requires_redirect_uris);

    const toggleGrantType = (grantType: string) => {
        if (isMandatoryGrantType(selectedType, grantType)) return;
        onGrantTypesChange(grantTypes.includes(grantType) ? grantTypes.filter(value => value !== grantType) : [...grantTypes, grantType]);
    };

    return (
        <div className="space-y-5 pt-2">
            <div className="space-y-2">
                <Label>Allowed grant types</Label>
                <div className="flex flex-wrap gap-2">
                    {selectedType.allowed_grant_types.map(grantType => {
                        const isActive = grantTypes.includes(grantType.type);
                        const isMandatory = isMandatoryGrantType(selectedType, grantType.type);

                        return (
                            <button
                                key={grantType.type}
                                type="button"
                                className={cn(
                                    'inline-flex items-center gap-1.5 rounded-lg border px-3 py-2 text-sm transition-colors',
                                    isActive ? 'border-primary bg-primary/10 text-primary' : 'border-border hover:bg-accent/50',
                                    isMandatory ? 'cursor-not-allowed opacity-80' : 'cursor-pointer',
                                )}
                                onClick={() => toggleGrantType(grantType.type)}
                                disabled={isMandatory}
                                aria-pressed={isActive}
                            >
                                {isActive && <CircleCheckIcon className="size-3.5" aria-hidden />}
                                {grantType.name}
                                {isMandatory && (
                                    <Badge variant="secondary" className="ml-1 text-xs">
                                        Mandatory
                                    </Badge>
                                )}
                            </button>
                        );
                    })}
                </div>
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
            />
        </div>
    );
}
