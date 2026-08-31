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

import { searchApis } from '../../api/catalog';
import type { Api } from '../../api/types';

export const FEATURED_AGENT_COUNT = 3;

const LOAD_ERROR = 'Unable to load featured agents. Please try again.';

export interface FeaturedAgentsState {
    agents: Api[];
    loading: boolean;
    error: string | null;
}

export function useFeaturedAgents(): FeaturedAgentsState {
    const [state, setState] = useState<FeaturedAgentsState>({
        agents: [],
        loading: true,
        error: null,
    });

    useEffect(() => {
        let cancelled = false;

        searchApis({ page: 1, size: FEATURED_AGENT_COUNT })
            .then(response => {
                if (!cancelled) {
                    setState({ agents: response.data ?? [], loading: false, error: null });
                }
            })
            .catch(() => {
                if (!cancelled) {
                    setState({ agents: [], loading: false, error: LOAD_ERROR });
                }
            });

        return () => {
            cancelled = true;
        };
    }, []);

    return state;
}
