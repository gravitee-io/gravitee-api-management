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
import { AccessDenied } from './components/AccessDenied';
import { EnvironmentSwitcher } from './components/EnvironmentSwitcher';
import { MockSettingsForm } from './components/MockSettingsForm';
import { PersonaSwitcher } from './components/PersonaSwitcher';
import { ReadOnlyBanner } from './components/ReadOnlyBanner';
import { SettingsSidebar } from './components/SettingsSidebar';
import { useSettingsContext } from './context/SettingsContext';
import { useSettingsPermissions } from './hooks/useSettingsPermissions';
import { ENVIRONMENTS } from './mock/environments';

export function SettingsModule() {
    const { state } = useSettingsContext();
    const { selectedItem, accessLevel } = useSettingsPermissions();

    const envName = ENVIRONMENTS.find(e => e.id === state.selectedEnvId)?.name ?? state.selectedEnvId ?? '';

    return (
        <>
            <SettingsSidebar />

            <div className="flex flex-wrap items-center justify-end gap-2 pb-4 border-b mb-4">
                <PersonaSwitcher />
            </div>

            {!selectedItem && <p className="text-muted-foreground">Select a settings item from the sidebar.</p>}

            {selectedItem && (
                <div>
                    <EnvironmentSwitcher />

                    {accessLevel === 'denied' && selectedItem.scope === 'env' && (
                        <AccessDenied itemLabel={selectedItem.label} envName={envName} />
                    )}

                    {accessLevel === 'read-only' && (
                        <>
                            <ReadOnlyBanner />
                            <MockSettingsForm item={selectedItem} disabled />
                        </>
                    )}

                    {accessLevel === 'full' && <MockSettingsForm item={selectedItem} disabled={false} />}
                </div>
            )}
        </>
    );
}
