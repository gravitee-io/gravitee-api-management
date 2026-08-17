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

import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { Input, Switch, Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@gravitee/graphene-core';
import { useEffect, useMemo, useState } from 'react';

import { OrgSettingsFormShell } from '../features/organization-settings/components/OrgSettingsFormShell';
import { useOrgConsoleSettings } from '../features/organization-settings/hooks/useOrgConsoleSettings';
import { useSaveOrgConsoleSettings } from '../features/organization-settings/hooks/useSaveOrgConsoleSettings';
import type { ConsoleSettings } from '../features/organization-settings/types/consoleSettings';
import { buildConsoleSettingsSavePayload } from '../features/organization-settings/utils/buildConsoleSettingsSavePayload';
import { isConsoleSettingReadonly } from '../features/organization-settings/utils/isConsoleSettingReadonly';

const SYSTEM_READONLY_TOOLTIP = 'Configuration provided by the system';

interface ManagementFormState {
    title: string;
    url: string;
    supportEnabled: boolean;
    userCreationEnabled: boolean;
    automaticValidationEnabled: boolean;
    tasks: string;
    notifications: string;
}

function buildState(settings: ConsoleSettings | undefined): ManagementFormState {
    return {
        title: settings?.management?.title ?? '',
        url: settings?.management?.url ?? '',
        supportEnabled: settings?.management?.support?.enabled ?? false,
        userCreationEnabled: settings?.management?.userCreation?.enabled ?? false,
        automaticValidationEnabled: settings?.management?.automaticValidation?.enabled ?? false,
        tasks: String(settings?.scheduler?.tasks ?? ''),
        notifications: String(settings?.scheduler?.notifications ?? ''),
    };
}

function parsePositiveInt(value: string): number | null {
    if (!/^\d+$/.test(value.trim())) return null;
    const parsed = Number(value);
    return parsed >= 1 ? parsed : null;
}

function ToggleRow({
    id,
    label,
    checked,
    disabled,
    systemReadonly,
    onToggle,
}: Readonly<{
    id: string;
    label: string;
    checked: boolean;
    disabled: boolean;
    systemReadonly: boolean;
    onToggle: (checked: boolean) => void;
}>) {
    const control = <Switch id={id} checked={checked} onCheckedChange={onToggle} disabled={disabled} aria-label={label} />;
    return (
        <div className="flex items-center justify-between gap-4 py-3">
            <label htmlFor={id} className={`text-sm font-medium ${disabled ? 'cursor-default' : 'cursor-pointer'}`}>
                {label}
            </label>
            {systemReadonly ? (
                <Tooltip>
                    <TooltipTrigger asChild>
                        <span className="inline-flex">{control}</span>
                    </TooltipTrigger>
                    <TooltipContent>{SYSTEM_READONLY_TOOLTIP}</TooltipContent>
                </Tooltip>
            ) : (
                control
            )}
        </div>
    );
}

export function ManagementAndSchedulersPage() {
    const canEdit = useHasPermission({ anyOf: ['organization-settings-u'] });
    const { data: settings, isLoading, isError } = useOrgConsoleSettings();
    const saveMutation = useSaveOrgConsoleSettings();

    const [localState, setLocalState] = useState<ManagementFormState>(() => buildState(settings));
    const [savedState, setSavedState] = useState<ManagementFormState>(() => buildState(settings));

    useEffect(() => {
        if (!settings) return;
        const next = buildState(settings);
        setLocalState(next);
        setSavedState(next);
    }, [settings]);

    const readonly = useMemo(
        () => ({
            title: isConsoleSettingReadonly(settings, 'management.title'),
            url: isConsoleSettingReadonly(settings, 'management.url'),
            support: isConsoleSettingReadonly(settings, 'console.support.enabled'),
            userCreation: isConsoleSettingReadonly(settings, 'console.userCreation.enabled'),
            automaticValidation: isConsoleSettingReadonly(settings, 'console.userCreation.automaticValidation.enabled'),
            tasks: isConsoleSettingReadonly(settings, 'console.scheduler.tasks'),
            notifications: isConsoleSettingReadonly(settings, 'console.scheduler.notifications'),
        }),
        [settings],
    );

    const isDirty = JSON.stringify(localState) !== JSON.stringify(savedState);
    const tasksValue = parsePositiveInt(localState.tasks);
    const notificationsValue = parsePositiveInt(localState.notifications);
    const isValid = tasksValue !== null && notificationsValue !== null;

    function handleSave() {
        if (!settings || !isDirty || !isValid || saveMutation.isPending) return;
        const payload = buildConsoleSettingsSavePayload(settings, 'management', {
            management: {
                title: localState.title,
                url: localState.url,
                support: { enabled: localState.supportEnabled },
                userCreation: { enabled: localState.userCreationEnabled },
                automaticValidation: { enabled: localState.userCreationEnabled ? localState.automaticValidationEnabled : false },
            },
            scheduler: { tasks: tasksValue, notifications: notificationsValue },
        });
        saveMutation.mutate(payload, { onSuccess: () => setSavedState(localState) });
    }

    return (
        <OrgSettingsFormShell
            title="Management & Schedulers"
            description="Name this organization console, decide who can register, and how often background tasks and notifications run."
            canEdit={canEdit}
            isDirty={isDirty}
            isValid={isValid}
            isSaving={saveMutation.isPending}
            isLoading={isLoading}
            isError={isError}
            onSave={handleSave}
            onDiscard={() => setLocalState(savedState)}
        >
            <TooltipProvider delayDuration={200}>
                <div className="space-y-6">
                    <section className="rounded-lg border p-4 space-y-4">
                        <h2 className="text-base font-semibold">Management</h2>
                        <div className="space-y-1.5">
                            <label htmlFor="management-title" className="text-sm font-medium">
                                Title
                            </label>
                            <Input
                                id="management-title"
                                value={localState.title}
                                onChange={e => setLocalState(prev => ({ ...prev, title: e.target.value }))}
                                disabled={!canEdit || readonly.title}
                            />
                        </div>
                        <div className="space-y-1.5">
                            <label htmlFor="management-url" className="text-sm font-medium">
                                Management URL
                            </label>
                            <Input
                                id="management-url"
                                value={localState.url}
                                onChange={e => setLocalState(prev => ({ ...prev, url: e.target.value }))}
                                disabled={!canEdit || readonly.url}
                            />
                        </div>
                        <ToggleRow
                            id="management-support"
                            label="Activate Support"
                            checked={localState.supportEnabled}
                            disabled={!canEdit || readonly.support}
                            systemReadonly={readonly.support}
                            onToggle={checked => setLocalState(prev => ({ ...prev, supportEnabled: checked }))}
                        />
                        <ToggleRow
                            id="management-user-creation"
                            label="Allow User Registration"
                            checked={localState.userCreationEnabled}
                            disabled={!canEdit || readonly.userCreation}
                            systemReadonly={readonly.userCreation}
                            onToggle={checked => setLocalState(prev => ({ ...prev, userCreationEnabled: checked }))}
                        />
                        {localState.userCreationEnabled ? (
                            <ToggleRow
                                id="management-automatic-validation"
                                label="Enable automatic validation of registration requests"
                                checked={localState.automaticValidationEnabled}
                                disabled={!canEdit || readonly.automaticValidation}
                                systemReadonly={readonly.automaticValidation}
                                onToggle={checked => setLocalState(prev => ({ ...prev, automaticValidationEnabled: checked }))}
                            />
                        ) : null}
                    </section>

                    <section className="rounded-lg border p-4 space-y-4">
                        <h2 className="text-base font-semibold">Schedulers</h2>
                        <div className="space-y-1.5">
                            <label htmlFor="scheduler-tasks" className="text-sm font-medium">
                                Tasks (in seconds)
                            </label>
                            <Input
                                id="scheduler-tasks"
                                type="number"
                                min={1}
                                value={localState.tasks}
                                onChange={e => setLocalState(prev => ({ ...prev, tasks: e.target.value }))}
                                disabled={!canEdit || readonly.tasks}
                                aria-invalid={tasksValue === null}
                            />
                        </div>
                        <div className="space-y-1.5">
                            <label htmlFor="scheduler-notifications" className="text-sm font-medium">
                                Notifications (in seconds)
                            </label>
                            <Input
                                id="scheduler-notifications"
                                type="number"
                                min={1}
                                value={localState.notifications}
                                onChange={e => setLocalState(prev => ({ ...prev, notifications: e.target.value }))}
                                disabled={!canEdit || readonly.notifications}
                                aria-invalid={notificationsValue === null}
                            />
                        </div>
                    </section>
                </div>
            </TooltipProvider>
        </OrgSettingsFormShell>
    );
}
