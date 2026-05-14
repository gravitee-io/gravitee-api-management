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
import { Button, cn, Skeleton } from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';

import { AlertsTab } from './alert-form/AlertsTab';
import { HistoryTab } from './alert-form/HistoryTab';
import { NotificationsTab } from './alert-form/NotificationsTab';
import { useAlertForm } from './alert-form/useAlertForm';

export function AlertFormPage() {
    const {
        isUpdate,
        canEdit,
        name,
        setName,
        description,
        setDescription,
        severity,
        setSeverity,
        enabled,
        setEnabled,
        ruleId,
        handleRuleChange,
        conditions,
        updateCondition,
        filters,
        addFilter,
        updateFilter,
        removeFilter,
        notifications,
        addNotification,
        removeNotification,
        updateNotificationTarget,
        timeframes,
        addTimeframe,
        removeTimeframe,
        toggleTimeframeDay,
        updateTimeframeHour,
        dampening,
        setDampening,
        errors,
        setErrors,
        activeTab,
        setActiveTab,
        isDirty,
        saveError,
        historyPage,
        isLoadingAlert,
        isPending,
        selectedRule,
        metricsForRule,
        handleSave,
        handleCancel,
        markDirty,
    } = useAlertForm();

    if (isUpdate && isLoadingAlert) {
        return (
            <div className="space-y-6 p-6">
                <Skeleton className="h-8 w-48 rounded" />
                <Skeleton className="h-10 w-full rounded" />
                <Skeleton className="h-64 w-full rounded-lg" />
            </div>
        );
    }

    return (
        <div className="space-y-6 p-6">
            {/* ─── Header ─────────────────────────────────────────────────── */}
            <div>
                <Button variant="ghost" size="sm" className="-ml-2 mb-3 text-muted-foreground" onClick={handleCancel}>
                    <ArrowLeftIcon className="size-4" />
                    Back to alerts
                </Button>
                <h1 className="text-2xl font-semibold">{isUpdate ? 'Update alert' : 'Create new alert'}</h1>
                <p className="mt-1 text-sm text-muted-foreground">
                    Configure your own alerts. Get notified when a metric condition is met, when data is missing, or when an endpoint health
                    check status changes.
                </p>
            </div>

            {/* ─── Save error ──────────────────────────────────────────────── */}
            {saveError && (
                <div
                    className="rounded-lg p-3"
                    style={{ background: 'hsl(var(--destructive) / 0.08)', border: '1px solid hsl(var(--destructive) / 0.3)' }}
                >
                    <p className="text-xs text-destructive">{saveError}</p>
                </div>
            )}

            <div>
                {/* ── Tab bar ──────────────────────────────────────────────── */}
                <div role="tablist" className="flex gap-1 border-b">
                    {(isUpdate ? (['alerts', 'notifications', 'history'] as const) : (['alerts', 'notifications'] as const)).map(tab => (
                        <button
                            key={tab}
                            role="tab"
                            type="button"
                            aria-selected={activeTab === tab}
                            onClick={() => setActiveTab(tab)}
                            className={cn(
                                '-mb-px border-b-2 px-4 py-2 text-sm font-medium transition-colors',
                                activeTab === tab
                                    ? 'border-primary text-foreground'
                                    : 'border-transparent text-muted-foreground hover:border-muted-foreground hover:text-foreground',
                            )}
                        >
                            {tab.charAt(0).toUpperCase() + tab.slice(1)}
                        </button>
                    ))}
                </div>

                {/* ── Tab panels ───────────────────────────────────────────── */}
                {activeTab === 'alerts' && (
                    <AlertsTab
                        name={name}
                        setName={setName}
                        description={description}
                        setDescription={setDescription}
                        severity={severity}
                        setSeverity={setSeverity}
                        enabled={enabled}
                        setEnabled={setEnabled}
                        ruleId={ruleId}
                        handleRuleChange={handleRuleChange}
                        isUpdate={isUpdate}
                        canEdit={canEdit}
                        errors={errors}
                        setErrors={setErrors}
                        markDirty={markDirty}
                        timeframes={timeframes}
                        addTimeframe={addTimeframe}
                        removeTimeframe={removeTimeframe}
                        toggleTimeframeDay={toggleTimeframeDay}
                        updateTimeframeHour={updateTimeframeHour}
                        conditions={conditions}
                        updateCondition={updateCondition}
                        metricsForRule={metricsForRule}
                        filters={filters}
                        addFilter={addFilter}
                        updateFilter={updateFilter}
                        removeFilter={removeFilter}
                        selectedRule={selectedRule}
                    />
                )}

                {activeTab === 'notifications' && (
                    <NotificationsTab
                        dampening={dampening}
                        setDampening={setDampening}
                        notifications={notifications}
                        addNotification={addNotification}
                        removeNotification={removeNotification}
                        updateNotificationTarget={updateNotificationTarget}
                        canEdit={canEdit}
                        markDirty={markDirty}
                    />
                )}

                {isUpdate && activeTab === 'history' && <HistoryTab historyPage={historyPage} />}
            </div>

            {/* ─── Save bar ────────────────────────────────────────────────── */}
            {canEdit && isDirty && (
                <div className="sticky bottom-0 z-10 -mx-6 flex items-center justify-end gap-3 border-t bg-background px-6 py-3">
                    <Button variant="outline" onClick={handleCancel}>
                        Cancel
                    </Button>
                    <Button onClick={handleSave} disabled={isPending}>
                        {isPending ? 'Saving…' : isUpdate ? 'Save' : 'Create'}
                    </Button>
                </div>
            )}

            {canEdit && !isDirty && !isUpdate && (
                <div className="flex items-center justify-end gap-3 pt-2">
                    <Button variant="outline" onClick={handleCancel}>
                        Cancel
                    </Button>
                    <Button onClick={handleSave} disabled={isPending}>
                        {isPending ? 'Creating…' : 'Create'}
                    </Button>
                </div>
            )}
        </div>
    );
}
