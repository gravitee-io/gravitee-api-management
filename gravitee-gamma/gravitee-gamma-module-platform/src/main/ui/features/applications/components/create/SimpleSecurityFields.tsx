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
import { Input, Label, Textarea } from '@gravitee/graphene-core';

interface SimpleSecurityFieldsProps {
    readonly appType: string;
    readonly appClientId: string;
    readonly clientCertificate: string;
    readonly onAppTypeChange: (value: string) => void;
    readonly onAppClientIdChange: (value: string) => void;
    readonly onClientCertificateChange: (value: string) => void;
}

export function SimpleSecurityFields({
    appType,
    appClientId,
    clientCertificate,
    onAppTypeChange,
    onAppClientIdChange,
    onClientCertificateChange,
}: SimpleSecurityFieldsProps) {
    return (
        <div className="space-y-5 pt-2">
            <div className="space-y-2">
                <Label htmlFor="simple-type">Type</Label>
                <Input
                    id="simple-type"
                    value={appType}
                    onChange={event => onAppTypeChange(event.target.value)}
                    placeholder="Type of the application (mobile, web, ...)"
                    maxLength={64}
                />
                <p className="text-xs text-muted-foreground">Type of the application (mobile, web, ...)</p>
            </div>

            <div className="space-y-2">
                <Label htmlFor="simple-client-id">Client ID</Label>
                <Input
                    id="simple-client-id"
                    value={appClientId}
                    onChange={event => onAppClientIdChange(event.target.value)}
                    placeholder="Client ID for API Key / JWT subscriptions"
                    maxLength={300}
                />
                <p className="text-xs text-muted-foreground">
                    The <code className="rounded bg-muted px-1 py-0.5 text-xs">client_id</code> of the application. This field is required
                    to subscribe to certain type of API Plan (OAuth2, JWT).
                </p>
            </div>

            <div className="space-y-2">
                <Label htmlFor="simple-client-certificate">Client Certificate (PEM Only)</Label>
                <Textarea
                    id="simple-client-certificate"
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
        </div>
    );
}
