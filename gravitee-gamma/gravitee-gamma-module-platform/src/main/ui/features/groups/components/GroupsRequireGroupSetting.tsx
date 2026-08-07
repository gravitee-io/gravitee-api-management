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

import { Button, Switch } from '@gravitee/graphene-core';
import { useEffect, useState } from 'react';

import { isUserGroupRequired, useConsoleSettings, useSetConsoleSettings, type ConsoleSettings } from '../../../shared/console-settings';
import { notify } from '../../../shared/notify';
import { saveOrgConsoleSettings } from '../../../shared/services/orgConsoleSettings';

/** Org-wide "require a group on applications" toggle — mirrors classic groups.component.ts/.html's
 *  settingsForm + gio-save-bar, gated behind organization-settings-r the same way (read gate only; the
 *  backend itself enforces the write permission on save, matching classic). Lives on the groups list page
 *  because that's where classic puts it too. */
export function GroupsRequireGroupSetting() {
    const settings = useConsoleSettings();
    const setConsoleSettings = useSetConsoleSettings();

    const [enabled, setEnabled] = useState(() => isUserGroupRequired(settings));
    const [isSaving, setIsSaving] = useState(false);

    useEffect(() => {
        setEnabled(isUserGroupRequired(settings));
    }, [settings]);

    const hasChanged = enabled !== isUserGroupRequired(settings);

    async function handleSave() {
        setIsSaving(true);
        try {
            // The backend replaces the whole settings document — spread everything already loaded and
            // override only this one nested field, or every other console setting would be wiped out.
            const payload: ConsoleSettings = {
                ...settings,
                userGroup: { ...settings?.userGroup, required: { ...settings?.userGroup?.required, enabled } },
            };
            const saved = await saveOrgConsoleSettings(payload);
            setConsoleSettings(saved);
            notify.success('Successfully updated groups settings.');
        } catch (error) {
            notify.error(error, 'Error occurred while saving groups settings.');
        } finally {
            setIsSaving(false);
        }
    }

    return (
        <div className="flex items-center justify-between gap-4 rounded-lg border px-4 py-3">
            <div className="flex items-center gap-3">
                <Switch id="require-user-group" checked={enabled} onCheckedChange={setEnabled} disabled={isSaving} />
                <div className="space-y-0.5">
                    <label htmlFor="require-user-group" className="text-sm font-medium cursor-pointer">
                        Requires an application to have at least one group added in order to create or update it.
                    </label>
                    <p className="text-xs text-muted-foreground">Use this setting if you want to enforce group ownership of applications.</p>
                </div>
            </div>
            <Button type="button" size="sm" onClick={handleSave} disabled={!hasChanged || isSaving}>
                {isSaving ? 'Saving…' : 'Save'}
            </Button>
        </div>
    );
}
