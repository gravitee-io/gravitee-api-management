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

import { useConsoleSettings } from '../../../shared/console-settings';
import { notify } from '../../../shared/notify';
import {
    ALERT_RULES,
    type AlertMetricDefinition,
    type AlertRuleCategory,
    type AlertRuleDefinition,
    canDefineAlertTemplate,
    getAlertRuleCategoriesForEnvironment,
    getAlertRuleLabel,
    getAlertRulesForEnvironment,
    getFilterMetricsForRuleId,
    getMetricsForRuleId,
    ruleIdToSourceType,
    sourceTypeToRuleId,
} from '../constants/alertConstants';
import {
    type AlertFormData,
    alertTriggerToFormData,
    createPlatformAlert,
    listPlatformAlertEvents,
    listPlatformAlerts,
    associatePlatformAlert,
    updatePlatformAlertFromForm,
} from '../services/alerts';
import { getNotifierSchema } from '../services/notifiers';
import type { AlertFormCondition, AlertFormNotification, AlertFormTimeframe, AlertHistoryPage, AlertRuleId, AlertSeverity } from '../types';
import { defaultFilterCondition, collectAlertFormErrors, isAlertFormReady } from '../utils/alertConditionComplete';
import { getDefaultCondition } from '../utils/alertDefaults';
import { ENVIRONMENT_ALERT_CREATE_PERMISSION, ENVIRONMENT_ALERT_UPDATE_PERMISSION } from '../utils/alertPermissions';
import { alertNotificationsIncompleteReason, areAlertNotificationsComplete } from '../utils/notifierSchema';
import { platformAlertKeys } from '../utils/queryKeys';

export interface UseAlertFormReturn {
    alertId: string | undefined;
    isUpdate: boolean;
    canEdit: boolean;

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
    notificationsComplete: boolean;
    notificationsIncompleteReason: string | null;
    selectedRule: AlertRuleDefinition | undefined;
    visibleRules: AlertRuleDefinition[];
    visibleRuleCategories: AlertRuleCategory[];
    ruleLabel: string;
    metricsForRule: AlertMetricDefinition[];
    filterMetrics: AlertMetricDefinition[];

    template: boolean;
    associateOnApiCreate: boolean;
    setTemplate: Dispatch<SetStateAction<boolean>>;
    setAssociateOnApiCreate: Dispatch<SetStateAction<boolean>>;
    associateToApis: () => void;
    isAssociating: boolean;
    isTemplateAlert: boolean;

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
    const { alertId } = useParams<{ alertId: string }>();
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const environmentId = env?.id ?? '';
    const consoleSettings = useConsoleSettings();
    const cloudHostedEnabled = consoleSettings?.cloudHosted?.enabled === true;

    const isUpdate = !!alertId;
    const canCreate = useHasPermission({ anyOf: [ENVIRONMENT_ALERT_CREATE_PERMISSION] });
    const canUpdate = useHasPermission({ anyOf: [ENVIRONMENT_ALERT_UPDATE_PERMISSION] });

    const [name, setName] = useState('New alert');
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
    const [template, setTemplate] = useState(false);
    const [associateOnApiCreate, setAssociateOnApiCreate] = useState(false);
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

    const {
        data: existingAlerts,
        isLoading: isLoadingAlert,
        isError: isAlertListError,
    } = useQuery({
        queryKey: platformAlertKeys.list(environmentId),
        queryFn: () => listPlatformAlerts(environmentId),
        enabled: isUpdate && !!environmentId,
        staleTime: 30_000,
    });

    const existingAlert = useMemo(
        () => (isUpdate && existingAlerts ? existingAlerts.find(a => a.id === alertId) : undefined),
        [isUpdate, existingAlerts, alertId],
    );
    const alertNotFound = isUpdate && !isLoadingAlert && !isAlertListError && !!existingAlerts && !existingAlert;
    const canEdit = (isUpdate ? canUpdate : canCreate) && !existingAlert?.template;

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
            setTemplate(!!fd.template);
            setAssociateOnApiCreate(
                Array.isArray(fd.event_rules) && fd.event_rules.some(rule => (rule as { event?: string }).event === 'API_CREATE'),
            );
            setHydrateError(false);
        } catch {
            setHydrateError(true);
        }
        initializedForRef.current = alertId;
    }, [existingAlert, alertId]);

    useEffect(() => {
        if (existingAlert?.template && activeTab === 'history') {
            setActiveTab('alerts');
        }
    }, [existingAlert?.template, activeTab]);

    const {
        data: historyPage,
        refetch: refetchHistory,
        isFetching: isRefreshingHistory,
    } = useQuery({
        queryKey: platformAlertKeys.history(environmentId, alertId ?? '', historyPageNumber, historyPageSize),
        queryFn: () => listPlatformAlertEvents(environmentId, alertId!, historyPageNumber - 1, historyPageSize),
        enabled: isUpdate && activeTab === 'history' && !!environmentId && !!alertId && !existingAlert?.template,
    });

    const notifierTypes = useMemo(() => [...new Set(notifications.map(n => n.type).filter(Boolean))], [notifications]);
    const schemaResults = useQueries({
        queries: notifierTypes.map(notifierId => ({
            queryKey: platformAlertKeys.notifierSchema(environmentId, notifierId),
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
        const schemasLoading = schemaResults.some(result => result.isLoading || (result.isFetching && !result.data));
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
                ? updatePlatformAlertFromForm(environmentId, alertId, data, {
                      template: existingAlert?.template,
                      event_rules: existingAlert?.event_rules,
                      projections: existingAlert?.projections,
                  })
                : createPlatformAlert(environmentId, data),
        onSuccess: saved => {
            queryClient.invalidateQueries({ queryKey: platformAlertKeys.list(environmentId) });
            notify.success(isUpdate ? `Alert "${saved.name}" updated.` : `Alert "${saved.name}" created.`);
            if (isUpdate) {
                setIsDirty(false);
                setActiveTab('alerts');
                setSaveError(null);
                return;
            }
            navigate('..');
        },
        onError: (e: Error) => {
            setSaveError(e.message || 'Failed to save alert.');
            notify.error(e, 'Failed to save alert.');
        },
    });

    const associateMutation = useMutation({
        mutationFn: () => associatePlatformAlert(environmentId, alertId!),
        onSuccess: () => {
            notify.success(`Alert "${name}" has been associated to all APIs`);
        },
        onError: (e: Error) => {
            notify.error(e, 'Failed to associate alert to APIs.');
        },
    });

    const selectedRule = useMemo(() => ALERT_RULES.find(r => r.source === source && r.type === type), [source, type]);
    const visibleRules = useMemo(
        () => getAlertRulesForEnvironment(cloudHostedEnabled, selectedRule?.id),
        [cloudHostedEnabled, selectedRule?.id],
    );
    const visibleRuleCategories = useMemo(
        () => getAlertRuleCategoriesForEnvironment(cloudHostedEnabled, selectedRule?.category),
        [cloudHostedEnabled, selectedRule?.category],
    );
    const metricsForRule = useMemo(() => (selectedRule ? getMetricsForRuleId(selectedRule.id) : []), [selectedRule]);
    const filterMetrics = useMemo(() => (selectedRule ? getFilterMetricsForRuleId(selectedRule.id) : []), [selectedRule]);
    const ruleLabel = selectedRule?.description ?? getAlertRuleLabel(source, type);

    const canSubmit = useMemo(
        () =>
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
        [name, isUpdate, selectedRule?.id, conditions, filters, notifications, notificationsComplete, dampening],
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
        mutation.mutate({
            name,
            description,
            severity,
            enabled: template ? false : enabled,
            source,
            type,
            conditions,
            filters,
            notifications,
            timeframes,
            dampening,
            template,
            event_rules: template && associateOnApiCreate ? [{ event: 'API_CREATE' }] : [],
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
        setDescription(nextRule?.description ?? '');
        if (!canDefineAlertTemplate(nextRule?.category)) {
            setTemplate(false);
            setAssociateOnApiCreate(false);
        }
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
    const setNotificationType = (index: number, type: string) => {
        setNotifications(prev => prev.map((n, i) => (i === index ? { type, configuration: {} } : n)));
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
        setTimeframes(prev => [...prev, { days: [1, 2, 3, 4, 5], startHour: 9 * 3600, endHour: 18 * 3600 }]);
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
        notificationsComplete,
        notificationsIncompleteReason,
        selectedRule,
        visibleRules,
        visibleRuleCategories,
        ruleLabel,
        metricsForRule,
        filterMetrics,
        template,
        associateOnApiCreate,
        setTemplate,
        setAssociateOnApiCreate,
        associateToApis: () => {
            void associateMutation.mutate();
        },
        isAssociating: associateMutation.isPending,
        isTemplateAlert: !!existingAlert?.template,
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
