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
import { useEnvironment, useHasPermission } from '@gravitee/gamma-modules-sdk';
import { useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query';
import type { Dispatch, SetStateAction } from 'react';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';

import {
    ALERT_RULES,
    type AlertMetricDefinition,
    type AlertRuleCategory,
    type AlertRuleDefinition,
    getAlertRuleCategoriesForApi,
    getAlertRuleLabel,
    getFilterMetricsForRuleId,
    getMetricsForRuleId,
    ruleIdToSourceType,
    sourceTypeToRuleId,
} from '../../../constants/alertConstants';
import { getNotifierSchema } from '../../../services/alertNotifiers';
import {
    type AlertFormData,
    alertTriggerToFormData,
    createAlertTrigger,
    getAlertHistory,
    getAlertStatus,
    listAlerts,
    updateAlertTrigger,
} from '../../../services/alerts';
import type {
    AlertFormCondition,
    AlertFormNotification,
    AlertFormTimeframe,
    AlertHistoryPage,
    AlertRuleId,
    AlertSeverity,
} from '../../../types';
import { collectAlertFormErrors, defaultFilterCondition, isAlertFormReady } from '../../../utils/alertConditionComplete';
import { getDefaultCondition } from '../../../utils/alertDefaults';
import { API_ALERT_CREATE_PERMISSION, API_ALERT_UPDATE_PERMISSION } from '../../../utils/alertPermissions';
import { alertNotificationsIncompleteReason, areAlertNotificationsComplete } from '../../../utils/notifierSchema';
import { apiAlertKeys } from '../../../utils/queryKeys';
import { END_OF_DAY_SECONDS } from '../../../utils/timeframeTime';

export interface UseAlertFormReturn {
    alertId: string | undefined;
    isUpdate: boolean;
    canEdit: boolean;
    hasAlertPlugins: boolean;

    name: string;
    description: string;
    severity: AlertSeverity;
    enabled: boolean;
    ruleId: AlertRuleId | undefined;
    conditions: AlertFormCondition[];
    filters: AlertFormCondition[];
    notifications: AlertFormNotification[];
    timeframes: AlertFormTimeframe[];
    dampening: AlertFormData['dampening'];
    errors: Record<string, string>;
    activeTab: string;
    isDirty: boolean;
    saveError: string | null;
    historyPage: AlertHistoryPage | undefined;
    historyPageNumber: number;
    historyPageSize: number;
    setHistoryPageNumber: Dispatch<SetStateAction<number>>;
    setHistoryPageSize: Dispatch<SetStateAction<number>>;
    isRefreshingHistory: boolean;
    isLoadingAlert: boolean;
    isAlertListError: boolean;
    hydrateError: boolean;
    alertNotFound: boolean;
    isPending: boolean;
    canSubmit: boolean;
    notificationsIncompleteReason: string | null;
    selectedRule: AlertRuleDefinition | undefined;
    visibleRuleCategories: AlertRuleCategory[];
    ruleLabel: string;
    metricsForRule: AlertMetricDefinition[];
    filterMetrics: AlertMetricDefinition[];

    setName: Dispatch<SetStateAction<string>>;
    setDescription: Dispatch<SetStateAction<string>>;
    setSeverity: Dispatch<SetStateAction<AlertSeverity>>;
    setEnabled: Dispatch<SetStateAction<boolean>>;
    setDampening: Dispatch<SetStateAction<AlertFormData['dampening']>>;
    setErrors: Dispatch<SetStateAction<Record<string, string>>>;
    setActiveTab: Dispatch<SetStateAction<string>>;

    handleSave: () => void;
    handleCancel: () => void;
    handleRuleChange: (newRuleId: AlertRuleId) => void;
    refreshHistory: () => void;
    markDirty: () => void;

    updateCondition: (index: number, c: AlertFormCondition) => void;
    addFilter: () => void;
    updateFilter: (index: number, f: AlertFormCondition) => void;
    removeFilter: (index: number) => void;
    addNotification: () => void;
    removeNotification: (index: number) => void;
    setNotificationType: (index: number, type: string) => void;
    updateNotification: (index: number, configuration: Record<string, unknown>) => void;
    addTimeframe: () => void;
    removeTimeframe: (index: number) => void;
    toggleTimeframeDay: (index: number, dayNum: number) => void;
    updateTimeframeHour: (index: number, field: 'startHour' | 'endHour', value: number) => void;
    setTimeframeDays: (index: number, days: number[]) => void;
    updateTimeframeHours: (index: number, startHour: number, endHour: number) => void;
}

export function useAlertForm(): UseAlertFormReturn {
    const { apiId, alertId } = useParams<{ apiId: string; alertId: string }>();
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const environmentId = env?.id ?? '';

    const isUpdate = !!alertId && alertId !== 'new';
    const canCreate = useHasPermission({ anyOf: [API_ALERT_CREATE_PERMISSION] });
    const canUpdate = useHasPermission({ anyOf: [API_ALERT_UPDATE_PERMISSION] });
    const canEdit = isUpdate ? canUpdate : canCreate;

    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [severity, setSeverity] = useState<AlertSeverity>('INFO');
    const [enabled, setEnabled] = useState(false);
    const [ruleId, setRuleId] = useState<AlertRuleId | undefined>(undefined);
    const [source, setSource] = useState('');
    const [type, setType] = useState('');
    const [conditions, setConditions] = useState<AlertFormCondition[]>([]);
    const [filters, setFilters] = useState<AlertFormCondition[]>([]);
    const [notifications, setNotifications] = useState<AlertFormNotification[]>([]);
    const [timeframes, setTimeframes] = useState<AlertFormTimeframe[]>([]);
    const [dampening, setDampening] = useState<AlertFormData['dampening']>({ mode: 'STRICT_COUNT', trueEvaluations: 1 });
    const [historyPageNumber, setHistoryPageNumber] = useState(1);
    const [historyPageSize, setHistoryPageSize] = useState(10);
    const [errors, setErrors] = useState<Record<string, string>>({});
    const allowedTabs = isUpdate ? ['alerts', 'notifications', 'history'] : ['alerts', 'notifications'];
    const tabFromUrl = searchParams.get('tab');
    const [activeTab, setActiveTab] = useState(tabFromUrl && allowedTabs.includes(tabFromUrl) ? tabFromUrl : 'alerts');
    const [isDirty, setIsDirty] = useState(false);
    const [saveError, setSaveError] = useState<string | null>(null);

    const initializedForRef = useRef<string | undefined>(undefined);
    const [hydrateError, setHydrateError] = useState(false);
    const markDirty = useCallback(() => setIsDirty(true), []);

    const { data: alertStatus } = useQuery({
        queryKey: apiAlertKeys.status(environmentId, apiId ?? ''),
        queryFn: () => getAlertStatus(environmentId, apiId!),
        enabled: !!environmentId && !!apiId,
    });
    const hasAlertPlugins = (alertStatus?.available_plugins ?? 0) > 0;

    const {
        data: existingAlerts,
        isLoading: isLoadingAlert,
        isError: isAlertListError,
    } = useQuery({
        queryKey: apiAlertKeys.list(environmentId, apiId ?? ''),
        queryFn: () => listAlerts(environmentId, apiId!),
        enabled: isUpdate && !!apiId,
        staleTime: 30_000,
    });

    const existingAlert = useMemo(
        () => (isUpdate && existingAlerts ? existingAlerts.find(a => a.id === alertId) : undefined),
        [isUpdate, existingAlerts, alertId],
    );
    const alertNotFound = isUpdate && !isLoadingAlert && !isAlertListError && !!existingAlerts && !existingAlert;

    useEffect(() => {
        if (!existingAlert || initializedForRef.current === alertId) return;
        try {
            const fd = alertTriggerToFormData(existingAlert);
            setName(fd.name);
            setDescription(fd.description);
            setSeverity(fd.severity);
            setEnabled(fd.enabled);
            setSource(fd.source);
            setType(fd.type);
            const mappedRuleId = sourceTypeToRuleId(fd.source, fd.type);
            if (mappedRuleId) {
                setRuleId(mappedRuleId);
            }
            setConditions(fd.conditions);
            setFilters(fd.filters);
            setNotifications(fd.notifications);
            setTimeframes(fd.timeframes);
            setDampening(fd.dampening ?? { mode: 'STRICT_COUNT', trueEvaluations: 1 });
            setHydrateError(false);
        } catch {
            setHydrateError(true);
        }
        initializedForRef.current = alertId;
    }, [existingAlert, alertId]);

    const {
        data: historyPage,
        refetch: refetchHistory,
        isFetching: isRefreshingHistory,
    } = useQuery({
        queryKey: apiAlertKeys.history(environmentId, apiId ?? '', alertId ?? '', historyPageNumber, historyPageSize),
        queryFn: () => getAlertHistory(environmentId, apiId!, alertId!, historyPageNumber - 1, historyPageSize),
        enabled: isUpdate && activeTab === 'history' && !!environmentId && !!apiId && !!alertId,
    });

    const notifierTypes = useMemo(() => [...new Set(notifications.map(n => n.type).filter(Boolean))], [notifications]);
    const schemaResults = useQueries({
        queries: notifierTypes.map(notifierId => ({
            queryKey: apiAlertKeys.notifierSchema(environmentId, notifierId),
            queryFn: () => getNotifierSchema(environmentId, notifierId),
            enabled: !!environmentId,
        })),
    });
    const notificationSchemaState = useMemo(() => {
        const schemas: Record<string, Record<string, unknown> | undefined> = {};
        const failedNotifierIds = new Set<string>();
        notifierTypes.forEach((id, index) => {
            schemas[id] = schemaResults[index]?.data;
            if (schemaResults[index]?.isError && !schemaResults[index]?.data) {
                failedNotifierIds.add(id);
            }
        });
        const schemasLoading = schemaResults.some(result => {
            const isFetchingWithoutData = result.isFetching && !result.data;
            return result.isLoading || isFetchingWithoutData;
        });
        return { schemas, failedNotifierIds, schemasLoading };
    }, [notifierTypes, schemaResults]);

    const notificationsComplete = useMemo(
        () =>
            areAlertNotificationsComplete(
                notifications,
                notificationSchemaState.schemas,
                notificationSchemaState.schemasLoading,
                notificationSchemaState.failedNotifierIds,
                { treatSchemaErrorAsComplete: isUpdate },
            ),
        [notifications, notificationSchemaState, isUpdate],
    );

    const notificationsIncompleteReason = useMemo(
        () =>
            alertNotificationsIncompleteReason(
                notifications,
                notificationSchemaState.schemas,
                notificationSchemaState.schemasLoading,
                notificationSchemaState.failedNotifierIds,
                { treatSchemaErrorAsComplete: isUpdate },
            ),
        [notifications, notificationSchemaState, isUpdate],
    );

    const mutation = useMutation({
        mutationFn: (data: AlertFormData) =>
            isUpdate && alertId
                ? updateAlertTrigger(environmentId, apiId!, alertId, data)
                : createAlertTrigger(environmentId, apiId!, data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiAlertKeys.list(environmentId, apiId ?? '') });
            navigate('..');
        },
        onError: (e: Error) => setSaveError(e.message || 'Failed to save alert.'),
    });

    const selectedRule = useMemo(() => ALERT_RULES.find(r => r.source === source && r.type === type), [source, type]);
    const visibleRuleCategories = useMemo(() => getAlertRuleCategoriesForApi(), []);
    const metricsForRule = useMemo(() => (selectedRule ? getMetricsForRuleId(selectedRule.id) : []), [selectedRule]);
    const filterMetrics = useMemo(() => (selectedRule ? getFilterMetricsForRuleId(selectedRule.id) : []), [selectedRule]);
    const ruleLabel = selectedRule?.description ?? getAlertRuleLabel(source, type);

    const canSubmit = useMemo(
        () =>
            hasAlertPlugins &&
            isAlertFormReady({
                name,
                isUpdate,
                ruleId: selectedRule?.id,
                conditions,
                filters,
                notifications,
                notificationsComplete,
                dampening,
            }),
        [hasAlertPlugins, name, isUpdate, selectedRule?.id, conditions, filters, notifications, notificationsComplete, dampening],
    );

    const handleSave = () => {
        const errs = collectAlertFormErrors({
            name,
            isUpdate,
            ruleId: selectedRule?.id,
            conditions,
            filters,
            notifications,
            notificationsComplete,
            dampening,
        });
        setErrors(errs);
        if (Object.keys(errs).length > 0) {
            setActiveTab(errs.name || errs.rule || errs.conditions || errs.filters ? 'alerts' : 'notifications');
            return;
        }
        const { source: nextSource, type: nextType } = ruleIdToSourceType(selectedRule!.id);
        mutation.mutate({
            name,
            description,
            severity,
            enabled,
            source: nextSource,
            type: nextType,
            conditions,
            filters,
            notifications,
            timeframes,
            dampening,
        });
    };

    const handleCancel = () => navigate('..');

    const handleRuleChange = (newRuleId: AlertRuleId) => {
        const nextSourceType = ruleIdToSourceType(newRuleId);
        setRuleId(newRuleId);
        setSource(nextSourceType.source);
        setType(nextSourceType.type);
        setConditions(getDefaultCondition(newRuleId));
        setFilters([]);
        const nextRule = ALERT_RULES.find(r => r.id === newRuleId);
        setDescription(prev => (prev.trim() ? prev : (nextRule?.description ?? '')));
        markDirty();
    };

    const updateCondition = useCallback(
        (index: number, c: AlertFormCondition) => {
            setConditions(prev => prev.map((item, i) => (i === index ? c : item)));
            markDirty();
        },
        [markDirty],
    );

    const addFilter = () => {
        const defaultProperty = filterMetrics[0]?.key ?? 'response.response_time';
        setFilters(prev => [...prev, defaultFilterCondition(defaultProperty)]);
        markDirty();
    };
    const updateFilter = useCallback(
        (index: number, f: AlertFormCondition) => {
            setFilters(prev => prev.map((item, i) => (i === index ? f : item)));
            markDirty();
        },
        [markDirty],
    );
    const removeFilter = useCallback(
        (index: number) => {
            setFilters(prev => prev.filter((_, i) => i !== index));
            markDirty();
        },
        [markDirty],
    );

    const addNotification = () => {
        setNotifications(prev => [...prev, { type: '', configuration: {} }]);
        markDirty();
    };
    const removeNotification = (index: number) => {
        setNotifications(prev => prev.filter((_, i) => i !== index));
        markDirty();
    };
    const setNotificationType = (index: number, notifierType: string) => {
        setNotifications(prev => prev.map((n, i) => (i === index ? { type: notifierType, configuration: {} } : n)));
        markDirty();
    };
    const updateNotification = useCallback(
        (index: number, configuration: Record<string, unknown>) => {
            setNotifications(prev => prev.map((n, i) => (i === index ? { ...n, configuration } : n)));
            markDirty();
        },
        [markDirty],
    );

    const addTimeframe = () => {
        setTimeframes(prev => [...prev, { days: [], startHour: 0, endHour: END_OF_DAY_SECONDS }]);
        markDirty();
    };
    const removeTimeframe = (index: number) => {
        setTimeframes(prev => prev.filter((_, i) => i !== index));
        markDirty();
    };
    const toggleTimeframeDay = useCallback(
        (index: number, dayNum: number) => {
            setTimeframes(prev =>
                prev.map((t, i) =>
                    i === index ? { ...t, days: t.days.includes(dayNum) ? t.days.filter(d => d !== dayNum) : [...t.days, dayNum] } : t,
                ),
            );
            markDirty();
        },
        [markDirty],
    );
    const updateTimeframeHour = useCallback(
        (index: number, field: 'startHour' | 'endHour', value: number) => {
            setTimeframes(prev => prev.map((t, i) => (i === index ? { ...t, [field]: value } : t)));
            markDirty();
        },
        [markDirty],
    );
    const setTimeframeDays = useCallback(
        (index: number, days: number[]) => {
            setTimeframes(prev => prev.map((t, i) => (i === index ? { ...t, days } : t)));
            markDirty();
        },
        [markDirty],
    );
    const updateTimeframeHours = useCallback(
        (index: number, startHour: number, endHour: number) => {
            setTimeframes(prev => prev.map((t, i) => (i === index ? { ...t, startHour, endHour } : t)));
            markDirty();
        },
        [markDirty],
    );

    return {
        alertId,
        isUpdate,
        canEdit,
        hasAlertPlugins,
        name,
        description,
        severity,
        enabled,
        ruleId,
        conditions,
        filters,
        notifications,
        timeframes,
        dampening,
        errors,
        activeTab,
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
        isPending: mutation.isPending,
        canSubmit,
        notificationsIncompleteReason,
        selectedRule,
        visibleRuleCategories,
        ruleLabel,
        metricsForRule,
        filterMetrics,
        setName,
        setDescription,
        setSeverity,
        setEnabled,
        setDampening,
        setErrors,
        setActiveTab,
        handleSave,
        handleCancel,
        handleRuleChange,
        refreshHistory: () => {
            void refetchHistory();
        },
        markDirty,
        updateCondition,
        addFilter,
        updateFilter,
        removeFilter,
        addNotification,
        removeNotification,
        setNotificationType,
        updateNotification,
        addTimeframe,
        removeTimeframe,
        toggleTimeframeDay,
        updateTimeframeHour,
        setTimeframeDays,
        updateTimeframeHours,
    };
}
