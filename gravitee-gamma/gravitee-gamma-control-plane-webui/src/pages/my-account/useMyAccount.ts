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
import { toast } from '@gravitee/graphene-core';
import { useCallback, useEffect, useMemo, useState } from 'react';

import { customFieldsAsStrings, isInternalUser, toUpdateUserPayload } from './myAccount.mapping';
import type { ConsoleAuthenticationSettings, ProfileDraft } from './myAccount.types';
import { deleteCurrentUser, fetchConsoleAuthentication, fetchCurrentUser, updateCurrentUser } from './services/currentUserAccount';
import { useLogout, useRefreshCurrentUser } from '../../features/auth';
import { useAuthStore } from '../../features/auth/auth.store';
import type { CurrentUser } from '../../features/auth/auth.types';
import { retainIdentityFields } from '../../features/auth/normalizeCurrentUser';
import { fetchCustomUserFields, type CustomUserField } from '../../features/auth/services/registration.service';
import { ApiError } from '../../shared/api/api-client';

function draftFromUser(user: CurrentUser): ProfileDraft {
    return {
        firstname: user.firstname ?? '',
        lastname: user.lastname ?? '',
        email: user.email ?? '',
        customFields: customFieldsAsStrings(user.customFields),
        pictureDataUrl: null,
        resetToDefault: false,
        // Classic stores originalPicture as getUserPicture() — the /user/avatar URL, which exists
        // whenever the user has an id. GET /user does not send a picture field.
        hadPictureOnLoad: Boolean(user.id),
    };
}

function errorMessage(error: unknown, fallback: string): string {
    if (error instanceof ApiError) {
        return error.message || fallback;
    }
    if (error instanceof Error && error.message) {
        return error.message;
    }
    return fallback;
}

export function useMyAccount() {
    const refreshCurrentUser = useRefreshCurrentUser();
    const logout = useLogout();

    const [user, setUser] = useState<CurrentUser | null>(null);
    const [draft, setDraft] = useState<ProfileDraft | null>(null);
    const [savedDraft, setSavedDraft] = useState<ProfileDraft | null>(null);
    const [fieldDefs, setFieldDefs] = useState<CustomUserField[]>([]);
    const [consoleAuth, setConsoleAuth] = useState<ConsoleAuthenticationSettings | undefined>(undefined);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<Error | null>(null);
    const [saving, setSaving] = useState(false);
    const [deleting, setDeleting] = useState(false);
    const [reloadTick, setReloadTick] = useState(0);

    const reload = useCallback(() => setReloadTick(tick => tick + 1), []);

    useEffect(() => {
        let cancelled = false;
        setLoading(true);
        setError(null);

        void (async () => {
            const [userResult, fieldsResult, authResult] = await Promise.allSettled([
                fetchCurrentUser(),
                fetchCustomUserFields(),
                fetchConsoleAuthentication(),
            ]);
            if (cancelled) {
                return;
            }
            if (userResult.status === 'rejected') {
                setError(userResult.reason instanceof Error ? userResult.reason : new Error(String(userResult.reason)));
                setLoading(false);
                return;
            }
            const nextUser = userResult.value;
            const nextDraft = draftFromUser(nextUser);
            setUser(nextUser);
            setDraft(nextDraft);
            setSavedDraft(nextDraft);
            setLoading(false);
            setFieldDefs(fieldsResult.status === 'fulfilled' ? fieldsResult.value : []);
            setConsoleAuth(authResult.status === 'fulfilled' ? authResult.value : undefined);
        })();

        return () => {
            cancelled = true;
        };
    }, [reloadTick]);

    const dirty = useMemo(() => JSON.stringify(draft) !== JSON.stringify(savedDraft), [draft, savedDraft]);
    const internal = isInternalUser(user?.source);

    const save = useCallback(async () => {
        if (!draft) {
            return;
        }
        setSaving(true);
        try {
            const saved = await updateCurrentUser(toUpdateUserPayload(draft));
            let nextUser = user ? retainIdentityFields(user, saved) : saved;
            try {
                nextUser = await refreshCurrentUser();
            } catch (refreshErr) {
                if (refreshErr instanceof Error && /signed out/i.test(refreshErr.message)) {
                    return;
                }
                useAuthStore.getState().applySessionUser(nextUser);
            }
            const nextDraft = draftFromUser(nextUser);
            setUser(nextUser);
            setDraft(nextDraft);
            setSavedDraft(nextDraft);
            toast.success('User has been updated successfully');
        } catch (err) {
            toast.error(errorMessage(err, 'Failed to update user'));
        } finally {
            setSaving(false);
        }
    }, [draft, refreshCurrentUser, user]);

    const cancel = useCallback(() => {
        if (savedDraft) {
            setDraft(savedDraft);
        }
    }, [savedDraft]);

    const deleteAccount = useCallback(async () => {
        setDeleting(true);
        try {
            await deleteCurrentUser();
        } catch (err) {
            toast.error(errorMessage(err, 'Failed to delete account'));
            setDeleting(false);
            return;
        }
        toast.success('You have been successfully deleted');
        try {
            await logout();
        } catch {
            // Account is already gone; leave deleting true so a retry cannot hit 404.
        }
    }, [logout]);

    return {
        user,
        draft,
        setDraft,
        fieldDefs,
        consoleAuth,
        loading,
        error,
        reload,
        dirty,
        internal,
        saving,
        deleting,
        save,
        cancel,
        deleteAccount,
    };
}
