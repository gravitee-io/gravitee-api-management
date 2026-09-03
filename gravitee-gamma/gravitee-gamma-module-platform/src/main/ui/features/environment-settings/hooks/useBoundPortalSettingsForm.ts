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
import { useEffect, useRef, useState } from 'react';

import type { PortalSettings } from '../../security-plan-types/services/portalSettings';

/**
 * Binds local form state to portal settings for the current environment.
 * Hydrates when `env.id` is present; skips clobbering in-progress edits on same-env refetch.
 */
export function useBoundPortalSettingsForm<T>(
    settings: PortalSettings | undefined,
    buildState: (settings: PortalSettings | undefined) => T,
    onCleanHydrate?: (settings: PortalSettings) => void,
) {
    const env = useEnvironment();
    const envId = env?.id;
    const [localState, setLocalState] = useState<T>(() => buildState(settings));
    const [savedState, setSavedState] = useState<T>(() => buildState(settings));
    const initializedForEnvId = useRef<string | null>(null);
    const isDirty = JSON.stringify(localState) !== JSON.stringify(savedState);
    const isDirtyRef = useRef(isDirty);
    isDirtyRef.current = isDirty;
    const onCleanHydrateRef = useRef(onCleanHydrate);
    onCleanHydrateRef.current = onCleanHydrate;

    useEffect(() => {
        if (!settings || !envId) {
            return;
        }
        const next = buildState(settings);
        if (initializedForEnvId.current !== envId) {
            const isEnvSwitch = initializedForEnvId.current !== null;
            initializedForEnvId.current = envId;
            if (isEnvSwitch || !isDirtyRef.current) {
                setLocalState(next);
                setSavedState(next);
                onCleanHydrateRef.current?.(settings);
            } else {
                setSavedState(next);
            }
            return;
        }
        setSavedState(next);
        if (!isDirtyRef.current) {
            setLocalState(next);
            onCleanHydrateRef.current?.(settings);
        }
    }, [settings, envId, buildState]);

    return { localState, setLocalState, savedState, setSavedState, isDirty };
}
