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
import { Button, cn, PageFocused, Skeleton } from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';

import { AlertsTab } from '../components/AlertsTab';
import { HistoryTab } from '../components/HistoryTab';
import { NotificationsTab } from '../components/NotificationsTab';
import { useAlertForm } from '../hooks/useAlertForm';

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
        setNotificationType,
        updateNotification,
        timeframes,
        addTimeframe,
        removeTimeframe,
        toggleTimeframeDay,
        updateTimeframeHour,
        setTimeframeDays,
        updateTimeframeHours,
        dampening,
        setDampening,
        errors,
        setErrors,
        activeTab,
        setActiveTab,
        isDirty,
        saveError,
        historyPage,
        historyPageNumber,
        historyPageSize,
        setHistoryPageNumber,
        setHistoryPageSize,
        isRefreshingHistory,
        isLoadingAlert,
        isAlertListError,
        hydrateError,
        alertNotFound,
        isPending,
        canSubmit,
        notificationsIncompleteReason,
        selectedRule,
        metricsForRule,
        filterMetrics,
        handleSave,
        handleCancel,
        markDirty,
        refreshHistory,
        ruleLabel,
        visibleRules,
        visibleRuleCategories,
        template,
        setTemplate,
        associateOnApiCreate,
        setAssociateOnApiCreate,
        associateToApis,
        isAssociating,
        isTemplateAlert,
    } = useAlertForm();

    if (isUpdate && isLoadingAlert) {
        return (
            <div className="space-y-6">
                <Skeleton className="h-8 w-48 rounded" />
                <Skeleton className="h-10 w-full rounded" />
                <Skeleton className="h-64 w-full rounded-lg" />
            </div>
        );
    }

    if (isUpdate && (isAlertListError || hydrateError)) {
        return (
            <div className="space-y-4">
                <Button variant="ghost" size="sm" className="-ml-2 text-muted-foreground" onClick={handleCancel}>
                    <ArrowLeftIcon className="size-4" />
                    Back to alerts
                </Button>
                <p className="text-sm text-destructive">Failed to load this alert. Please try again.</p>
            </div>
        );
    }

    if (alertNotFound) {
        return (
            <div className="space-y-4">
                <Button variant="ghost" size="sm" className="-ml-2 text-muted-foreground" onClick={handleCancel}>
                    <ArrowLeftIcon className="size-4" />
                    Back to alerts
                </Button>
                <h1 className="text-2xl font-semibold">Alert not found</h1>
                <p className="text-sm text-muted-foreground">This alert does not exist or was deleted.</p>
            </div>
        );
    }

    return (
        <PageFocused>
            <div className="space-y-6">
                {/* ─── Header ─────────────────────────────────────────────────── */}
                <div>
                    <Button variant="ghost" size="sm" className="-ml-2 mb-3 text-muted-foreground" onClick={handleCancel}>
                        <ArrowLeftIcon className="size-4" />
                        Back to alerts
                    </Button>
                    <h1 className="text-2xl font-semibold">{isUpdate ? 'Update alert' : 'Create new alert'}</h1>
                    <p className="mt-1 text-sm text-muted-foreground">
                        Configure your own alerts. Get notified when a metric condition is met, when data is missing, or when an endpoint
                        health check status changes.
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
                        {(isUpdate && !isTemplateAlert
                            ? (['alerts', 'notifications', 'history'] as const)
                            : (['alerts', 'notifications'] as const)
                        ).map(tab => (
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
                            setTimeframeDays={setTimeframeDays}
                            updateTimeframeHours={updateTimeframeHours}
                            conditions={conditions}
                            updateCondition={updateCondition}
                            metricsForRule={metricsForRule}
                            filterMetrics={filterMetrics}
                            filters={filters}
                            addFilter={addFilter}
                            updateFilter={updateFilter}
                            removeFilter={removeFilter}
                            selectedRule={selectedRule}
                            ruleLabel={ruleLabel}
                            rules={visibleRules}
                            ruleCategories={visibleRuleCategories}
                            template={template}
                            setTemplate={setTemplate}
                            associateOnApiCreate={associateOnApiCreate}
                            setAssociateOnApiCreate={setAssociateOnApiCreate}
                            onAssociateToApis={associateToApis}
                            isAssociating={isAssociating}
                        />
                    )}

                    {activeTab === 'notifications' && (
                        <NotificationsTab
                            dampening={dampening}
                            setDampening={setDampening}
                            notifications={notifications}
                            addNotification={addNotification}
                            removeNotification={removeNotification}
                            setNotificationType={setNotificationType}
                            updateNotification={updateNotification}
                            canEdit={canEdit}
                            markDirty={markDirty}
                            channelError={errors.notifications}
                            dampeningError={errors.dampening}
                        />
                    )}

                    {isUpdate && !isTemplateAlert && activeTab === 'history' && (
                        <HistoryTab
                            historyPage={historyPage}
                            onRefresh={refreshHistory}
                            isRefreshing={isRefreshingHistory}
                            page={historyPageNumber}
                            pageSize={historyPageSize}
                            onPageChange={setHistoryPageNumber}
                            onPageSizeChange={size => {
                                setHistoryPageSize(size);
                                setHistoryPageNumber(1);
                            }}
                        />
                    )}
                </div>

                {/* ─── Save bar ────────────────────────────────────────────────── */}
                {canEdit && isDirty && (
                    <div className="sticky bottom-0 z-10 -mx-6 flex items-center justify-end gap-3 border-t bg-background px-6 py-3">
                        {notificationsIncompleteReason && (
                            <p className="mr-auto text-xs text-destructive">{notificationsIncompleteReason}</p>
                        )}
                        <Button variant="outline" onClick={handleCancel}>
                            Cancel
                        </Button>
                        <Button onClick={handleSave} disabled={isPending || !canSubmit}>
                            {isPending ? 'Saving…' : isUpdate ? 'Save' : 'Create'}
                        </Button>
                    </div>
                )}

                {canEdit && !isDirty && !isUpdate && (
                    <div className="flex items-center justify-end gap-3 pt-2">
                        {notificationsIncompleteReason && (
                            <p className="mr-auto text-xs text-destructive">{notificationsIncompleteReason}</p>
                        )}
                        <Button variant="outline" onClick={handleCancel}>
                            Cancel
                        </Button>
                        <Button onClick={handleSave} disabled={isPending || !canSubmit}>
                            {isPending ? 'Creating…' : 'Create'}
                        </Button>
                    </div>
                )}
            </div>
        </PageFocused>
    );
}
