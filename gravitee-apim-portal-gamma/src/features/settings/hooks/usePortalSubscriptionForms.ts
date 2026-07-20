/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { useCallback, useEffect, useState } from 'react';

import {
    createSubscriptionForm,
    deleteSubscriptionForm,
    getSubscriptionForm,
    getSubscriptionFormsByPortalId,
    setSubscriptionFormMappedApis,
    updateSubscriptionForm,
} from '../storage/portal-subscription-forms.storage';
import type { FormField, MappedApi, SubscriptionForm } from '../types';

export function usePortalSubscriptionForms(portalId: string | undefined) {
    const [forms, setForms] = useState<SubscriptionForm[]>([]);
    const [loading, setLoading] = useState(true);

    const refresh = useCallback(async () => {
        if (!portalId) {
            setForms([]);
            setLoading(false);
            return;
        }

        setLoading(true);
        try {
            setForms(await getSubscriptionFormsByPortalId(portalId));
        } finally {
            setLoading(false);
        }
    }, [portalId]);

    useEffect(() => {
        void refresh();
    }, [refresh]);

    const addForm = useCallback(
        async (input: { name: string; description?: string }) => {
            if (!portalId) {
                return undefined;
            }
            const created = await createSubscriptionForm(portalId, input);
            await refresh();
            return created;
        },
        [portalId, refresh],
    );

    const removeForm = useCallback(
        async (formId: string) => {
            await deleteSubscriptionForm(formId);
            await refresh();
        },
        [refresh],
    );

    const updateMappedApis = useCallback(
        async (formId: string, mappedApis: readonly MappedApi[]) => {
            await setSubscriptionFormMappedApis(formId, mappedApis);
            await refresh();
        },
        [refresh],
    );

    const saveFields = useCallback(
        async (formId: string, fields: readonly FormField[]) => {
            const updated = await updateSubscriptionForm(formId, { fields: [...fields] });
            await refresh();
            return updated;
        },
        [refresh],
    );

    return {
        forms,
        loading,
        refresh,
        addForm,
        removeForm,
        updateMappedApis,
        saveFields,
    };
}

export function usePortalSubscriptionForm(portalId: string | undefined, formId: string | undefined) {
    const [form, setForm] = useState<SubscriptionForm | undefined>();
    const [loading, setLoading] = useState(true);
    const [missing, setMissing] = useState(false);

    const refresh = useCallback(async () => {
        if (!portalId || !formId) {
            setForm(undefined);
            setMissing(true);
            setLoading(false);
            return;
        }

        setLoading(true);
        try {
            const loaded = await getSubscriptionForm(formId);
            const belongsToPortal = loaded?.portalId === portalId;
            setForm(belongsToPortal ? loaded : undefined);
            setMissing(!belongsToPortal);
        } finally {
            setLoading(false);
        }
    }, [portalId, formId]);

    useEffect(() => {
        void refresh();
    }, [refresh]);

    const updateMappedApis = useCallback(
        async (mappedApis: readonly MappedApi[]) => {
            if (!formId) {
                return undefined;
            }
            const updated = await setSubscriptionFormMappedApis(formId, mappedApis);
            if (updated) {
                setForm(updated);
            }
            return updated;
        },
        [formId],
    );

    const saveFields = useCallback(
        async (fields: readonly FormField[]) => {
            if (!formId) {
                return undefined;
            }
            const updated = await updateSubscriptionForm(formId, { fields: [...fields] });
            if (updated) {
                setForm(updated);
            }
            return updated;
        },
        [formId],
    );

    return { form, loading, missing, refresh, updateMappedApis, saveFields };
}
