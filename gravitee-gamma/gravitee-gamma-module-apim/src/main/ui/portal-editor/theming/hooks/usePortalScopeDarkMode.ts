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
import { useCallback, useEffect, useRef, useState, type RefCallback } from 'react';

export function usePortalScopeDarkMode(): { ref: RefCallback<HTMLElement>; isDark: boolean } {
    const [isDark, setIsDark] = useState(false);
    const observerRef = useRef<MutationObserver | null>(null);

    const ref = useCallback((node: HTMLElement | null) => {
        observerRef.current?.disconnect();
        observerRef.current = null;

        if (!node) {
            return;
        }

        const scope = node.closest('.portal-scope');
        if (!scope) {
            setIsDark(false);
            return;
        }

        const sync = () => setIsDark(scope.classList.contains('dark'));
        sync();

        const observer = new MutationObserver(sync);
        observer.observe(scope, { attributes: true, attributeFilter: ['class'] });
        observerRef.current = observer;
    }, []);

    useEffect(() => () => observerRef.current?.disconnect(), []);

    return { ref, isDark };
}
