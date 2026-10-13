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
import { useEffect, useRef, useState } from 'react';

/**
 * Follows `active`, and keeps it true for at least `minimumMs` each time it turns true:
 * a state that lasts a few milliseconds would blink on screen without being read.
 */
export function useMinimumDuration(active: boolean, minimumMs: number): boolean {
    const [held, setHeld] = useState(false);
    const release = useRef<ReturnType<typeof setTimeout>>(undefined);

    useEffect(() => {
        if (!active) return;
        setHeld(true);
        clearTimeout(release.current);
        // Not cleared when `active` turns false: this timer is what holds the value.
        release.current = setTimeout(() => setHeld(false), minimumMs);
    }, [active, minimumMs]);

    useEffect(() => () => clearTimeout(release.current), []);

    return active || held;
}
