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

import type { DictionaryLifecycleState, DictionaryType } from '../types/dictionary';

const STATE_LABEL: Record<DictionaryLifecycleState, string> = {
    STARTED: 'Started',
    STOPPED: 'Stopped',
};

export function DictionaryTypeLabel({
    type,
    state,
}: Readonly<{
    type: DictionaryType;
    state?: DictionaryLifecycleState;
}>) {
    if (type === 'MANUAL') {
        return <span className="text-sm text-muted-foreground">Manual</span>;
    }

    const stateLabel = state ? (STATE_LABEL[state] ?? state) : undefined;
    const stateClassName = state === 'STARTED' ? 'text-green-600' : 'text-muted-foreground';

    return (
        <span className="text-sm text-muted-foreground">
            Dynamic
            {stateLabel ? (
                <>
                    {' '}
                    <span aria-hidden>·</span> <span className={stateClassName}>{stateLabel}</span>
                </>
            ) : null}
        </span>
    );
}
