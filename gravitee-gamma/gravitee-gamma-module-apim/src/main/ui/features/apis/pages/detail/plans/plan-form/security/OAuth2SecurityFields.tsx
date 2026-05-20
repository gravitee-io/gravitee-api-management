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

export interface OAuth2Config {
    authorizationServerResource: string;
    requiredScopes: string;
    strictMode: boolean;
    propagateToken: boolean;
}

export const DEFAULT_OAUTH2_CONFIG: OAuth2Config = {
    authorizationServerResource: '',
    requiredScopes: '',
    strictMode: false,
    propagateToken: false,
};

interface OAuth2SecurityFieldsProps {
    value: OAuth2Config;
    onChange: (v: OAuth2Config) => void;
    readOnly?: boolean;
}

export function OAuth2SecurityFields({ value, onChange, readOnly = false }: Readonly<OAuth2SecurityFieldsProps>) {
    return (
        <div className="space-y-4">
            <div className="space-y-2">
                <Label htmlFor="oauth2-resource">Authorization server resource</Label>
                <Input
                    id="oauth2-resource"
                    value={value.authorizationServerResource}
                    onChange={e => onChange({ ...value, authorizationServerResource: e.target.value })}
                    placeholder="Name of the OAuth2 resource configured on this API"
                    disabled={readOnly}
                />
                <p className="text-xs text-muted-foreground">
                    The name of the OAuth2 authorization server resource configured in your API&apos;s Resources section.
                </p>
            </div>

            <div className="space-y-2">
                <Label htmlFor="oauth2-scopes">Required scopes</Label>
                <Input
                    id="oauth2-scopes"
                    value={value.requiredScopes}
                    onChange={e => onChange({ ...value, requiredScopes: e.target.value })}
                    placeholder="Comma-separated scopes (e.g. read:api, write:api)"
                    disabled={readOnly}
                />
                <p className="text-xs text-muted-foreground">Comma-separated list of OAuth2 scopes that must be present in the token.</p>
            </div>

            <div className="flex items-center justify-between rounded-lg border px-4 py-3">
                <div className="space-y-0.5">
                    <Label htmlFor="oauth2-strict" className="text-sm font-medium">
                        Strict scope validation
                    </Label>
                    <p className="text-xs text-muted-foreground">
                        All listed scopes must be present in the token (AND). When off, any scope matches (OR).
                    </p>
                </div>
                <Switch
                    id="oauth2-strict"
                    checked={value.strictMode}
                    onCheckedChange={checked => onChange({ ...value, strictMode: checked })}
                    disabled={readOnly}
                />
            </div>

            <div className="flex items-center justify-between rounded-lg border px-4 py-3">
                <div className="space-y-0.5">
                    <Label htmlFor="oauth2-propagate" className="text-sm font-medium">
                        Propagate token to upstream
                    </Label>
                    <p className="text-xs text-muted-foreground">Forward the Authorization header to the backend after validation.</p>
                </div>
                <Switch
                    id="oauth2-propagate"
                    checked={value.propagateToken}
                    onCheckedChange={checked => onChange({ ...value, propagateToken: checked })}
                    disabled={readOnly}
                />
            </div>
        </div>
    );
}
