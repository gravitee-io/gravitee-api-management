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
import { Badge, Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@gravitee/graphene-core';

import { useSettingsContext } from '../context/SettingsContext';
import { useSettingsPermissions } from '../hooks/useSettingsPermissions';

export function EnvironmentSwitcher() {
    const { state, selectEnv } = useSettingsContext();
    const { userEnvironments, selectedItem } = useSettingsPermissions();

    if (!selectedItem || selectedItem.scope === 'org') return null;

    if (userEnvironments.length <= 1) {
        const envName = userEnvironments[0]?.name ?? 'None';
        return (
            <div className="flex items-center gap-2 mb-4">
                <span className="text-sm font-medium text-muted-foreground">Environment:</span>
                <Badge variant="secondary">{envName}</Badge>
            </div>
        );
    }

    return (
        <div className="flex items-center gap-2 mb-4">
            <span className="text-sm font-medium text-muted-foreground">Environment:</span>
            <Select value={state.selectedEnvId ?? ''} onValueChange={selectEnv}>
                <SelectTrigger className="w-[200px]" size="sm">
                    <SelectValue />
                </SelectTrigger>
                <SelectContent>
                    {userEnvironments.map(env => (
                        <SelectItem key={env.id} value={env.id}>
                            {env.name}
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>
        </div>
    );
}
