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
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@gravitee/graphene-core';

import { useSettingsContext } from '../context/SettingsContext';
import { PERSONAS } from '../mock/personas';

export function PersonaSwitcher() {
    const { state, setPersona } = useSettingsContext();

    return (
        <div className="flex items-center gap-2">
            <span className="text-sm font-medium text-muted-foreground whitespace-nowrap">Simulate User:</span>
            <Select
                value={state.persona.id}
                onValueChange={id => {
                    const persona = PERSONAS.find(p => p.id === id);
                    if (persona) setPersona(persona);
                }}
            >
                <SelectTrigger className="w-[220px]" size="sm">
                    <SelectValue />
                </SelectTrigger>
                <SelectContent>
                    {PERSONAS.map(p => (
                        <SelectItem key={p.id} value={p.id}>
                            {p.name} ({p.description})
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>
        </div>
    );
}
