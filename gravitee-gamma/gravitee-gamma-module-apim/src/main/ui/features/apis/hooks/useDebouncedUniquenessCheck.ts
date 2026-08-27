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
import { useEffect, useRef } from 'react';

import { useApiCreation } from '../store/apiCreationStore';

const DEBOUNCE_MS = 500;

interface VerificationResult {
    ok: boolean;
    reason?: string;
}

interface UseDebouncedUniquenessCheckOptions {
    /** Effect dependency key — a string that changes whenever the value being checked changes. */
    depsKey: string;
    /** Skip verification entirely (e.g. local validation hasn't passed yet, or this field doesn't apply to the current mode/protocol). */
    skip: boolean;
    /** `validationErrors` key this check reports against. */
    field: string;
    /** Shown when the API rejects the value without a `reason`. */
    fallbackMessage: string;
    /** Dispatched once verification succeeds — use it to clear any stale error for `field` (e.g. `UPDATE_FORM` with the field's own current value). */
    onVerified: () => void;
    /** Performs the actual network call once the debounce delay has elapsed. */
    verify: (environmentId: string) => Promise<VerificationResult>;
}

/**
 * Shared skeleton behind the wizard's async "is this host/path already taken" checks
 * (`useVerifyContextPath`, `useVerifyTcpHosts`): debounce, call `verify`, and write the
 * result into store validation errors — extracted so the two near-identical hooks don't
 * each carry their own copy of the debounce/dispatch bookkeeping.
 */
export function useDebouncedUniquenessCheck({
    depsKey,
    skip,
    field,
    fallbackMessage,
    onVerified,
    verify,
}: UseDebouncedUniquenessCheckOptions) {
    const { dispatch } = useApiCreation();
    const env = useEnvironment();
    const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    useEffect(() => {
        if (timerRef.current) clearTimeout(timerRef.current);

        dispatch({ type: 'SET_PATH_VERIFYING', value: false });

        if (!env || skip) return;

        dispatch({ type: 'SET_PATH_VERIFYING', value: true });

        timerRef.current = setTimeout(async () => {
            try {
                const result = await verify(env.id);
                if (!result.ok) {
                    dispatch({ type: 'SET_FIELD_ERROR', field, message: result.reason ?? fallbackMessage });
                } else {
                    onVerified();
                }
            } catch {
                // Network/server error: don't block the user — backend enforces on submit.
            } finally {
                dispatch({ type: 'SET_PATH_VERIFYING', value: false });
            }
        }, DEBOUNCE_MS);

        return () => {
            if (timerRef.current) clearTimeout(timerRef.current);
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [depsKey, skip, env?.id]);
}
