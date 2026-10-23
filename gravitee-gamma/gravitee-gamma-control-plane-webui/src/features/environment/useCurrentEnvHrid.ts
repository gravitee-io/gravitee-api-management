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
import { useEnvironmentStore } from './environment.store';
import { getPrimaryHrid, useEnvHrid } from './environment.utils';

/**
 * The environment of the address or, on a page above the environments, the one the session runs in: the last one
 * visited, or the first one after a reload.
 */
export function useCurrentEnvHrid(): string {
    const envHrid = useEnvHrid();
    const currentEnvironment = useEnvironmentStore(s => s.currentEnvironment);
    return envHrid || (currentEnvironment ? getPrimaryHrid(currentEnvironment) : '');
}
