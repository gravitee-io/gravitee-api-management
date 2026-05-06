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
import { createContext, useCallback, useContext, useMemo, useReducer, type ReactNode } from 'react';

import { DEFAULT_PERSONA, type MockPersona } from '../mock/personas';

interface SettingsState {
    readonly persona: MockPersona;
    readonly selectedItemKey: string;
    readonly selectedEnvId: string | null;
}

type SettingsAction =
    | { type: 'SET_PERSONA'; persona: MockPersona }
    | { type: 'SELECT_ITEM'; key: string }
    | { type: 'SELECT_ENV'; envId: string | null }
    | { type: 'RESET_SELECTION'; itemKey: string; envId: string | null };

function settingsReducer(state: SettingsState, action: SettingsAction): SettingsState {
    switch (action.type) {
        case 'SET_PERSONA':
            return { ...state, persona: action.persona, selectedItemKey: '', selectedEnvId: null };
        case 'SELECT_ITEM':
            return { ...state, selectedItemKey: action.key };
        case 'SELECT_ENV':
            return { ...state, selectedEnvId: action.envId };
        case 'RESET_SELECTION':
            return { ...state, selectedItemKey: action.itemKey, selectedEnvId: action.envId };
    }
}

interface SettingsContextValue {
    readonly state: SettingsState;
    readonly setPersona: (persona: MockPersona) => void;
    readonly selectItem: (key: string) => void;
    readonly selectEnv: (envId: string | null) => void;
    readonly resetSelection: (itemKey: string, envId: string | null) => void;
}

const SettingsContext = createContext<SettingsContextValue | null>(null);

const INITIAL_STATE: SettingsState = {
    persona: DEFAULT_PERSONA,
    selectedItemKey: '',
    selectedEnvId: null,
};

export function SettingsProvider({ children }: { readonly children: ReactNode }) {
    const [state, dispatch] = useReducer(settingsReducer, INITIAL_STATE);

    const setPersona = useCallback((persona: MockPersona) => {
        dispatch({ type: 'SET_PERSONA', persona });
    }, []);

    const selectItem = useCallback((key: string) => {
        dispatch({ type: 'SELECT_ITEM', key });
    }, []);

    const selectEnv = useCallback((envId: string | null) => {
        dispatch({ type: 'SELECT_ENV', envId });
    }, []);

    const resetSelection = useCallback((itemKey: string, envId: string | null) => {
        dispatch({ type: 'RESET_SELECTION', itemKey, envId });
    }, []);

    const value = useMemo<SettingsContextValue>(
        () => ({
            state,
            setPersona,
            selectItem,
            selectEnv,
            resetSelection,
        }),
        [state, setPersona, selectItem, selectEnv, resetSelection],
    );

    return <SettingsContext value={value}>{children}</SettingsContext>;
}

export function useSettingsContext(): SettingsContextValue {
    const ctx = useContext(SettingsContext);
    if (!ctx) {
        throw new Error('useSettingsContext must be used within a SettingsProvider');
    }
    return ctx;
}
