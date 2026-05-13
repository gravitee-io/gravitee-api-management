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

import { verifyContextPath } from '../services/apiProxy';
import { useApiCreation } from '../store/apiCreationStore';
import { validateContextPath } from '../utils/apiCreationValidation';

const DEBOUNCE_MS = 500;

/**
 * Watches `form.contextPath` and, when virtual hosts are disabled, fires a
 * debounced uniqueness check against the gateway (matching legacy console
 * webui behaviour). Writes the result directly into store validation errors.
 */
export function useVerifyContextPath() {
    const { state, dispatch } = useApiCreation();
    const env = useEnvironment();
    const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    const contextPath = state.form.contextPath;
    const virtualHostsEnabled = state.form.virtualHostsEnabled;

    useEffect(() => {
        if (timerRef.current) clearTimeout(timerRef.current);

        dispatch({ type: 'SET_PATH_VERIFYING', value: false });

        if (!env || virtualHostsEnabled || validateContextPath(contextPath) !== null) return;

        dispatch({ type: 'SET_PATH_VERIFYING', value: true });

        timerRef.current = setTimeout(async () => {
            try {
                const result = await verifyContextPath(env.id, [{ path: contextPath }]);
                if (!result.ok) {
                    dispatch({
                        type: 'SET_FIELD_ERROR',
                        field: 'contextPath',
                        message: result.reason ?? 'This context path is already in use by another API.',
                    });
                } else {
                    dispatch({ type: 'UPDATE_FORM', patch: { contextPath } });
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
    }, [contextPath, virtualHostsEnabled, env?.id]);
}
