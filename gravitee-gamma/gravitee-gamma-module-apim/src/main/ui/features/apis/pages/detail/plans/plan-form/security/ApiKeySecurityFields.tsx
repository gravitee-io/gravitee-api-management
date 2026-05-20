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
import { Input, Label, Switch } from '@gravitee/graphene-core';

export interface ApiKeyConfig {
    propagateApiKey: boolean;
    authorizationHeader: string;
}

export const DEFAULT_API_KEY_CONFIG: ApiKeyConfig = {
    propagateApiKey: false,
    authorizationHeader: 'X-Gravitee-Api-Key',
};

interface ApiKeySecurityFieldsProps {
    value: ApiKeyConfig;
    onChange: (v: ApiKeyConfig) => void;
    readOnly?: boolean;
}

export function ApiKeySecurityFields({ value, onChange, readOnly = false }: Readonly<ApiKeySecurityFieldsProps>) {
    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between rounded-lg border px-4 py-3">
                <div className="space-y-0.5">
                    <Label htmlFor="propagate-api-key" className="text-sm font-medium">
                        Propagate API Key to upstream API
                    </Label>
                    <p className="text-xs text-muted-foreground">
                        Forward the API Key header to the backend service after gateway validation.
                    </p>
                </div>
                <Switch
                    id="propagate-api-key"
                    checked={value.propagateApiKey}
                    onCheckedChange={checked => onChange({ ...value, propagateApiKey: checked })}
                    disabled={readOnly}
                />
            </div>

            <div className="space-y-2">
                <Label htmlFor="custom-api-key-header">Custom API Key header</Label>
                <Input
                    id="custom-api-key-header"
                    value={value.authorizationHeader}
                    onChange={e => onChange({ ...value, authorizationHeader: e.target.value })}
                    placeholder="X-Gravitee-Api-Key"
                    disabled={readOnly}
                />
                <p className="text-xs text-muted-foreground">
                    The header name used to pass the API key. Leave default for X-Gravitee-Api-Key.
                </p>
            </div>
        </div>
    );
}
