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
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { Dispatch, SetStateAction } from 'react';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';

import {
    ALERT_RULES,
    API_METRICS,
    type AlertMetricDefinition,
    type AlertRuleDefinition,
    getMetricsForRuleId,
    ruleIdToSourceType,
    sourceTypeToRuleId,
} from '../../features/apis/constants/alertConstants';
import type {
    AlertFormCondition,
    AlertFormNotification,
    AlertFormTimeframe,
    AlertHistoryPage,
    AlertNotificationChannel,
    AlertRuleId,
    AlertSeverity,
} from '../../features/apis/types/api';
import { apiAlertKeys } from '../../features/apis/utils/queryKeys';
import {
    type AlertFormData,
    alertTriggerToFormData,
    createAlertTrigger,
    getAlertHistory,
    listAlerts,
    updateAlertTrigger,
} from '../../services/alerts/alerts';

function getDefaultCondition(ruleId: AlertRuleId): AlertFormCondition[] {
    switch (ruleId) {
        case 'REQUEST@METRICS_SIMPLE_CONDITION':
            return [{ type: 'THRESHOLD', property: 'response.response_time', operator: 'GT' }];
        case 'REQUEST@MISSING_DATA':
            return [{ type: 'MISSING_DATA', timeUnit: 'MINUTES' }];
        case 'REQUEST@METRICS_AGGREGATION':
            return [
                {
                    type: 'AGGREGATION',
                    property: 'response.response_time',
                    aggregationFunction: 'AVG',
                    operator: 'GT',
                    timeUnit: 'MINUTES',
                },
            ];
        case 'REQUEST@METRICS_RATE':
            return [{ type: 'RATE', property: 'response.status', operator: 'GTE', rateOperator: 'GT', timeUnit: 'MINUTES' }];
        case 'ENDPOINT_HEALTH_CHECK@API_HC_ENDPOINT_STATUS_CHANGED':
        default:
            return [];
    }
}

export interface UseAlertFormReturn {
    alertId: string | undefined;
    isUpdate: boolean;
    canEdit: boolean;

    name: string;
    description: string;
    severity: AlertSeverity;
    enabled: boolean;
    ruleId: AlertRuleId;
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
    isLoadingAlert: boolean;
    isPending: boolean;
    selectedRule: AlertRuleDefinition | undefined;
    metricsForRule: AlertMetricDefinition[];

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
    markDirty: () => void;

    updateCondition: (index: number, c: AlertFormCondition) => void;
    addFilter: () => void;
    updateFilter: (index: number, f: AlertFormCondition) => void;
    removeFilter: (index: number) => void;
    addNotification: (channel: AlertNotificationChannel) => void;
    removeNotification: (index: number) => void;
    updateNotificationTarget: (index: number, target: string) => void;
    addTimeframe: () => void;
    removeTimeframe: (index: number) => void;
    toggleTimeframeDay: (index: number, dayNum: number) => void;
    updateTimeframeHour: (index: number, field: 'startHour' | 'endHour', value: number) => void;
}

export function useAlertForm(): UseAlertFormReturn {
    const { apiId, alertId } = useParams<{ apiId: string; alertId: string }>();
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const env = useEnvironment();
    const queryClient = useQueryClient();

    const isUpdate = !!alertId && alertId !== 'new';
    const canEdit = useHasPermission({ anyOf: ['api-definition-u'] });

    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [severity, setSeverity] = useState<AlertSeverity>('INFO');
    const [enabled, setEnabled] = useState(true);
    const [ruleId, setRuleId] = useState<AlertRuleId>('REQUEST@METRICS_SIMPLE_CONDITION');
    const [conditions, setConditions] = useState<AlertFormCondition[]>(getDefaultCondition('REQUEST@METRICS_SIMPLE_CONDITION'));
    const [filters, setFilters] = useState<AlertFormCondition[]>([]);
    const [notifications, setNotifications] = useState<AlertFormNotification[]>([]);
    const [timeframes, setTimeframes] = useState<AlertFormTimeframe[]>([]);
    const [dampening, setDampening] = useState<AlertFormData['dampening']>({ mode: 'STRICT_COUNT', trueEvaluations: 1 });
    const [errors, setErrors] = useState<Record<string, string>>({});
    const [activeTab, setActiveTab] = useState(searchParams.get('tab') || 'alerts');
    const [isDirty, setIsDirty] = useState(false);
    const [saveError, setSaveError] = useState<string | null>(null);

    const initializedForRef = useRef<string | undefined>(undefined);
    const markDirty = useCallback(() => setIsDirty(true), []);

    const { data: existingAlerts, isLoading: isLoadingAlert } = useQuery({
        queryKey: apiAlertKeys.list(env?.id ?? '', apiId ?? ''),
        queryFn: () => listAlerts(env?.id ?? '', apiId!),
        enabled: isUpdate && !!apiId,
    });

    const existingAlert = useMemo(
        () => (isUpdate && existingAlerts ? existingAlerts.find(a => a.id === alertId) : undefined),
        [isUpdate, existingAlerts, alertId],
    );

    useEffect(() => {
        if (!existingAlert || initializedForRef.current === alertId) return;
        initializedForRef.current = alertId;
        const fd = alertTriggerToFormData(existingAlert);
        setName(fd.name);
        setDescription(fd.description);
        setSeverity(fd.severity);
        setEnabled(fd.enabled);
        setRuleId(sourceTypeToRuleId(fd.source, fd.type));
        setConditions(fd.conditions);
        setFilters(fd.filters);
        setNotifications(fd.notifications);
        setTimeframes(fd.timeframes);
        setDampening(fd.dampening ?? { mode: 'STRICT_COUNT', trueEvaluations: 1 });
    }, [existingAlert, alertId]);

    const { data: historyPage } = useQuery({
        queryKey: apiAlertKeys.history(env?.id ?? '', apiId ?? '', alertId ?? ''),
        queryFn: () => getAlertHistory(env?.id ?? '', apiId!, alertId!),
        enabled: isUpdate && activeTab === 'history' && !!apiId && !!alertId,
    });

    const mutation = useMutation({
        mutationFn: (data: AlertFormData) =>
            isUpdate && alertId
                ? updateAlertTrigger(env?.id ?? '', apiId!, alertId, data)
                : createAlertTrigger(env?.id ?? '', apiId!, data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiAlertKeys.list(env?.id ?? '', apiId ?? '') });
            navigate('..');
        },
        onError: (e: Error) => setSaveError(e.message || 'Failed to save alert.'),
    });

    const validate = (): boolean => {
        const errs: Record<string, string> = {};
        if (!name.trim()) errs.name = 'Name is required.';
        else if (name.length < 3) errs.name = 'Name has to be at least 3 characters long.';
        else if (name.length > 50) errs.name = 'Name length must not exceed 50 characters.';
        setErrors(errs);
        return Object.keys(errs).length === 0;
    };

    const handleSave = () => {
        if (!validate()) {
            setActiveTab('alerts');
            return;
        }
        const { source, type } = ruleIdToSourceType(ruleId);
        mutation.mutate({ name, description, severity, enabled, source, type, conditions, filters, notifications, timeframes, dampening });
    };

    const handleCancel = () => navigate('..');

    const handleRuleChange = (newRuleId: AlertRuleId) => {
        setRuleId(newRuleId);
        setConditions(getDefaultCondition(newRuleId));
        setFilters([]);
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
        setFilters(prev => [...prev, { type: 'THRESHOLD', property: API_METRICS[0].key }]);
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

    const addNotification = (channel: AlertNotificationChannel) => {
        setNotifications(prev => [...prev, { channel, target: '' }]);
        markDirty();
    };
    const removeNotification = (index: number) => {
        setNotifications(prev => prev.filter((_, i) => i !== index));
        markDirty();
    };
    const updateNotificationTarget = (index: number, target: string) => {
        setNotifications(prev => prev.map((n, i) => (i === index ? { ...n, target } : n)));
        markDirty();
    };

    const addTimeframe = () => {
        setTimeframes(prev => [...prev, { days: [1, 2, 3, 4, 5], startHour: 9, endHour: 18 }]);
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

    const metricsForRule = useMemo(() => getMetricsForRuleId(ruleId), [ruleId]);
    const selectedRule = useMemo(() => ALERT_RULES.find(r => r.id === ruleId), [ruleId]);

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
        isLoadingAlert,
        isPending: mutation.isPending,
        selectedRule,
        metricsForRule,
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
        markDirty,
        updateCondition,
        addFilter,
        updateFilter,
        removeFilter,
        addNotification,
        removeNotification,
        updateNotificationTarget,
        addTimeframe,
        removeTimeframe,
        toggleTimeframeDay,
        updateTimeframeHour,
    };
}
