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
import { useCallback, useEffect, useRef, useState } from 'react';

import { verifyApiHosts } from '../services/apiProxy';
import type { TcpHostEntry } from '../types/apiCreation';
import { validateDuplicateHost } from '../utils/duplicateDialogValidation';

const DEBOUNCE_MS = 250;
const HOST_IN_USE = 'This host is already in use by another API.';
const VERIFY_FAILED = 'Unable to verify this host. Save stays disabled until the check succeeds.';

export function useTcpHostUniqueness(apiId: string | undefined, hosts: TcpHostEntry[]) {
    const env = useEnvironment();
    const [errorsById, setErrorsById] = useState<Record<string, string>>({});
    const [pending, setPending] = useState(false);
    const [dirtyIds, setDirtyIds] = useState<ReadonlySet<string>>(() => new Set());
    const generationRef = useRef(0);

    const markDirty = useCallback((id: string) => {
        setDirtyIds(previous => {
            if (previous.has(id)) return previous;
            const next = new Set(previous);
            next.add(id);
            return next;
        });
    }, []);

    const reset = useCallback(() => {
        generationRef.current += 1;
        setDirtyIds(previous => (previous.size === 0 ? previous : new Set()));
        setErrorsById(previous => (Object.keys(previous).length === 0 ? previous : {}));
        setPending(false);
    }, []);

    useEffect(() => {
        const generation = ++generationRef.current;
        const environmentId = env?.id;
        const hostsToVerify = hosts.filter(host => dirtyIds.has(host.id) && validateDuplicateHost(host.host) === null);

        setErrorsById(previous => (Object.keys(previous).length === 0 ? previous : {}));

        if (!environmentId || !apiId || hostsToVerify.length === 0) {
            setPending(false);
            return;
        }

        setPending(true);
        const timer = setTimeout(() => {
            void Promise.all(
                hostsToVerify.map(async host => {
                    try {
                        const result = await verifyApiHosts(environmentId, 'TCP', [host.host.trim()], apiId);
                        return { id: host.id, message: result.ok ? null : (result.reason ?? HOST_IN_USE) };
                    } catch {
                        return { id: host.id, message: VERIFY_FAILED };
                    }
                }),
            ).then(results => {
                if (generationRef.current !== generation) return;
                setErrorsById(previous => applyVerificationResults(previous, results));
                setPending(false);
            });
        }, DEBOUNCE_MS);

        return () => {
            clearTimeout(timer);
            generationRef.current += 1;
        };
    }, [apiId, env?.id, hosts, dirtyIds]);

    return { errorsById, pending, markDirty, reset };
}

function applyVerificationResults(
    previous: Record<string, string>,
    results: { id: string; message: string | null }[],
): Record<string, string> {
    const next = { ...previous };
    for (const result of results) {
        if (result.message) next[result.id] = result.message;
        else delete next[result.id];
    }
    return next;
}
