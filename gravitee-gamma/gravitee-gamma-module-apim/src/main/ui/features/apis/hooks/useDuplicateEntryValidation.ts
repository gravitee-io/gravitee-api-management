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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { useCallback, useRef, useState } from 'react';

import { verifyApiHosts, verifyContextPath } from '../services/apiProxy';
import { validateContextPath } from '../utils/apiCreationValidation';
import type { DuplicateEntryMode } from '../utils/apiGeneralDuplicate';
import { validateDuplicateHost } from '../utils/duplicateDialogValidation';

const DEBOUNCE_MS = 250;

/**
 * Sync + debounced async validation for duplicate dialog entry fields (classic console parity).
 * Verify APIs are called without apiId — same as contextPathAsyncValidator / hostAsyncValidator on duplicate dialog.
 */
export function useDuplicateEntryValidation(entryMode: DuplicateEntryMode) {
    const env = useEnvironment();
    const [contextPathError, setContextPathError] = useState<string | null>(null);
    const [hostError, setHostError] = useState<string | null>(null);
    const [verifying, setVerifying] = useState(false);
    const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    const resetValidation = useCallback(() => {
        if (timerRef.current) clearTimeout(timerRef.current);
        setContextPathError(null);
        setHostError(null);
        setVerifying(false);
    }, []);

    const verifyContextPathNow = useCallback(
        async (path: string): Promise<string | null> => {
            const syncError = validateContextPath(path);
            if (syncError) return syncError;
            if (!env?.id) return null;

            try {
                const result = await verifyContextPath(env.id, [{ path }]);
                return result.ok ? null : (result.reason ?? 'This context path is already in use by another API.');
            } catch {
                return null;
            }
        },
        [env],
    );

    const verifyHostNow = useCallback(
        async (value: string): Promise<string | null> => {
            const syncError = validateDuplicateHost(value);
            if (syncError) return syncError;
            if (!env?.id) return null;

            try {
                const result = await verifyApiHosts(env.id, 'TCP', [value]);
                return result.ok ? null : (result.reason ?? 'This host is already in use by another API.');
            } catch {
                return null;
            }
        },
        [env],
    );

    const scheduleContextPathValidation = useCallback(
        (path: string) => {
            if (timerRef.current) clearTimeout(timerRef.current);

            const syncError = validateContextPath(path);
            if (syncError) {
                setContextPathError(syncError);
                setVerifying(false);
                return;
            }

            setContextPathError(null);
            if (!env?.id) return;

            setVerifying(true);
            timerRef.current = setTimeout(() => {
                void verifyContextPathNow(path).then(err => {
                    setContextPathError(err);
                    setVerifying(false);
                });
            }, DEBOUNCE_MS);
        },
        [env, verifyContextPathNow],
    );

    const scheduleHostValidation = useCallback(
        (value: string) => {
            if (timerRef.current) clearTimeout(timerRef.current);

            const syncError = validateDuplicateHost(value);
            if (syncError) {
                setHostError(syncError);
                setVerifying(false);
                return;
            }

            setHostError(null);
            if (!env?.id) return;

            setVerifying(true);
            timerRef.current = setTimeout(() => {
                void verifyHostNow(value).then(err => {
                    setHostError(err);
                    setVerifying(false);
                });
            }, DEBOUNCE_MS);
        },
        [env, verifyHostNow],
    );

    const entryValid =
        entryMode === 'none' || (entryMode === 'contextPath' && contextPathError === null) || (entryMode === 'host' && hostError === null);

    return {
        contextPathError,
        hostError,
        verifying,
        entryValid,
        resetValidation,
        scheduleContextPathValidation,
        scheduleHostValidation,
        verifyContextPathNow,
        verifyHostNow,
        setContextPathError,
        setHostError,
    };
}
