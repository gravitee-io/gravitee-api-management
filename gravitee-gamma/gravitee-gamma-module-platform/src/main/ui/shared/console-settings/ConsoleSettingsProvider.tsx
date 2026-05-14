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
import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';

import type { ConsoleSettings } from './types';
import { fetchOrgConsoleSettings } from '../services/orgConsoleSettings';

const LOAD_ERROR_MESSAGE = 'Management API unreachable or error occurs, please check logs';

interface ConsoleSettingsContextValue {
    settings: ConsoleSettings | null;
    isReady: boolean;
}

const ConsoleSettingsContext = createContext<ConsoleSettingsContextValue | null>(null);

interface ConsoleSettingsProviderProps {
    readonly children: ReactNode;
}

/**
 * Platform bootstrap for GET /organizations/{orgId}/console.
 * Blocks child routes until settings load (APIM console strictness); failures stay inside platform only.
 */
export function ConsoleSettingsProvider({ children }: ConsoleSettingsProviderProps) {
    const [settings, setSettings] = useState<ConsoleSettings | null>(null);
    const [isReady, setIsReady] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<Error | null>(null);

    useEffect(() => {
        let cancelled = false;

        (async () => {
            try {
                const loaded = await fetchOrgConsoleSettings();
                if (cancelled) return;
                setSettings(loaded);
                setIsReady(true);
                setError(null);
            } catch (err) {
                if (cancelled) return;
                setError(err instanceof Error ? err : new Error(String(err)));
            } finally {
                if (!cancelled) {
                    setIsLoading(false);
                }
            }
        })();

        return () => {
            cancelled = true;
        };
    }, []);

    const value = useMemo(() => ({ settings, isReady }), [settings, isReady]);

    if (isLoading) {
        return (
            <div className="flex items-center justify-center p-8">
                <p className="text-sm text-muted-foreground">Loading organization settings…</p>
            </div>
        );
    }

    if (error) {
        return (
            <div className="flex items-center justify-center p-8">
                <p className="text-sm text-destructive">{LOAD_ERROR_MESSAGE}</p>
            </div>
        );
    }

    return <ConsoleSettingsContext.Provider value={value}>{children}</ConsoleSettingsContext.Provider>;
}

function useConsoleSettingsContext(): ConsoleSettingsContextValue {
    const value = useContext(ConsoleSettingsContext);
    if (!value) {
        throw new Error('useConsoleSettings must be used within ConsoleSettingsProvider');
    }
    return value;
}

export function useConsoleSettings(): ConsoleSettings | null {
    return useConsoleSettingsContext().settings;
}

export function useConsoleSettingsReady(): boolean {
    return useConsoleSettingsContext().isReady;
}
