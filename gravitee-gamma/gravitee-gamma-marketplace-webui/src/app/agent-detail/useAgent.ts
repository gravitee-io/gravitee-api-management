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
import { useEffect, useState } from 'react';

import { getApi } from '../../api/agent';
import { ApiError } from '../../api/portal-client';
import type { Api } from '../../api/types';

const NOT_FOUND = 'This agent could not be found.';
const LOAD_ERROR = 'Unable to load this agent. Please try again.';

export interface AgentState {
    agent: Api | null;
    loading: boolean;
    error: string | null;
}

export function useAgent(apiId: string | undefined): AgentState {
    const [state, setState] = useState<AgentState>({ agent: null, loading: true, error: null });

    useEffect(() => {
        if (!apiId) {
            setState({ agent: null, loading: false, error: NOT_FOUND });
            return;
        }

        let cancelled = false;
        setState(current => ({ ...current, loading: true, error: null }));

        getApi(apiId)
            .then(agent => {
                if (!cancelled) {
                    setState({ agent, loading: false, error: null });
                }
            })
            .catch(error => {
                if (cancelled) {
                    return;
                }
                const message = error instanceof ApiError && error.status === 404 ? NOT_FOUND : LOAD_ERROR;
                setState({ agent: null, loading: false, error: message });
            });

        return () => {
            cancelled = true;
        };
    }, [apiId]);

    return state;
}
