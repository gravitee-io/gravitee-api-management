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
import { useMemo } from 'react';
import { useLocation } from 'react-router-dom';

const ENV_PATH_RE = /\/environments\/([^/]+)/;

/**
 * Resolves the active environment id. Order of precedence:
 *  1. The shared SDK singleton (when the host has propagated the environment).
 *  2. The `:envHrid` segment of the current URL — host routes follow
 *     `/environments/:envHrid/apim/...`, so we can recover it even when the SDK
 *     hasn't been wired up yet (e.g. before the host's environment sync runs).
 *  3. `DEFAULT` as a last-resort fallback.
 */
export function useEnvironmentId(): string {
    const env = useEnvironment();
    const { pathname } = useLocation();

    return useMemo(() => {
        if (env?.id) return env.id;
        const match = ENV_PATH_RE.exec(pathname);
        return match?.[1] ?? 'DEFAULT';
    }, [env?.id, pathname]);
}
