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
import { Input, Label, Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@gravitee/graphene-core';

import { CollapsibleSection, SwitchRow } from '../../../../../components/CollapsibleSection';
import type { ProxyFormState } from '../types';

// ─── Export ───────────────────────────────────────────────────────────────────

interface ProxySectionProps {
    proxy: ProxyFormState;
    onChange: (patch: Partial<ProxyFormState>) => void;
    disabled?: boolean;
}

export function ProxySection({ proxy, onChange, disabled }: Readonly<ProxySectionProps>) {
    return (
        <CollapsibleSection title="Proxy">
            <SwitchRow
                id="dp-proxy-enabled"
                label="Enable proxy"
                desc="Route HTTP requests to the dynamic property source through a proxy."
                checked={proxy.enabled}
                onChange={v => onChange({ enabled: v })}
                disabled={disabled}
            />

            {proxy.enabled && (
                <>
                    <SwitchRow
                        id="dp-proxy-system"
                        label="Use system proxy"
                        desc="Inherit proxy settings from the gateway JVM system properties."
                        checked={proxy.useSystemProxy}
                        onChange={v => onChange({ useSystemProxy: v })}
                        disabled={disabled}
                    />

                    {!proxy.useSystemProxy && (
                        <div className="grid grid-cols-2 gap-4">
                            <div className="col-span-2 space-y-1.5">
                                <Label htmlFor="dp-proxy-type" className="text-sm">
                                    Proxy type
                                </Label>
                                <Select value={proxy.type} onValueChange={v => onChange({ type: v as ProxyFormState['type'] })}>
                                    <SelectTrigger id="dp-proxy-type" className="w-full">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="HTTP">HTTP</SelectItem>
                                        <SelectItem value="SOCKS4">SOCKS4</SelectItem>
                                        <SelectItem value="SOCKS5">SOCKS5</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>
                            <div className="space-y-1.5">
                                <Label htmlFor="dp-proxy-host" className="text-sm">
                                    Host
                                </Label>
                                <Input
                                    id="dp-proxy-host"
                                    value={proxy.host}
                                    placeholder="proxy.example.com"
                                    onChange={e => onChange({ host: e.target.value })}
                                    disabled={disabled}
                                />
                            </div>
                            <div className="space-y-1.5">
                                <Label htmlFor="dp-proxy-port" className="text-sm">
                                    Port
                                </Label>
                                <Input
                                    id="dp-proxy-port"
                                    type="number"
                                    value={proxy.port}
                                    placeholder="3128"
                                    onChange={e => onChange({ port: e.target.value })}
                                    disabled={disabled}
                                />
                            </div>
                            <div className="space-y-1.5">
                                <Label htmlFor="dp-proxy-username" className="text-sm">
                                    Username
                                </Label>
                                <Input
                                    id="dp-proxy-username"
                                    value={proxy.username}
                                    placeholder="Optional"
                                    onChange={e => onChange({ username: e.target.value })}
                                    disabled={disabled}
                                />
                            </div>
                            <div className="space-y-1.5">
                                <Label htmlFor="dp-proxy-password" className="text-sm">
                                    Password
                                </Label>
                                <Input
                                    id="dp-proxy-password"
                                    type="password"
                                    value={proxy.password}
                                    placeholder="Optional"
                                    onChange={e => onChange({ password: e.target.value })}
                                    disabled={disabled}
                                />
                            </div>
                        </div>
                    )}
                </>
            )}
        </CollapsibleSection>
    );
}
