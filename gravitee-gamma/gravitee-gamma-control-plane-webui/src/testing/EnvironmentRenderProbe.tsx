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
import type { ReactNode } from 'react';
import { useLocation } from 'react-router-dom';

import { useEnvironmentStore } from '../features/environment/environment.store';

/**
 * Records the (pathname, store environment) pair on *every* render, for tests asserting that the
 * two never disagree where children can observe them.
 *
 * Deliberately not built on {@link PathnameProbe}: that one reports from an effect keyed on the
 * pathname, so a render whose pathname is unchanged never reaches it. The renders this probe exists
 * to catch are exactly those, a mismatched pair immediately corrected under the same pathname, so
 * recording has to happen during render.
 */
export function EnvironmentRenderProbe({
    onRender,
    children,
}: {
    readonly onRender: (pathname: string, environmentId: string) => void;
    readonly children?: ReactNode;
}) {
    const { pathname } = useLocation();
    const environmentId = useEnvironmentStore(s => s.environmentId);

    onRender(pathname, environmentId);

    return <>{children}</>;
}
