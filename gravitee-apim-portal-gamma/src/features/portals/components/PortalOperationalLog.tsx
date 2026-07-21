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
import { Card, CardContent } from '@gravitee/graphene-core';

import { DUMMY_OPERATIONAL_LOG } from '../storage/dummy-dashboard-stats';

export function PortalOperationalLog() {
    return (
        <Card className="flex flex-col">
            <CardContent className="flex h-full flex-col gap-3 pt-5 pb-4">
                <div>
                    <p className="text-sm font-semibold">Recent Operational Log</p>
                </div>
                <ul className="space-y-4">
                    {DUMMY_OPERATIONAL_LOG.map(entry => (
                        <li key={entry.id} className="flex items-center gap-3">
                            <span
                                className="size-2 shrink-0 rounded-full bg-primary"
                                aria-hidden
                            />
                            <p className="min-w-0 flex-1 truncate text-sm text-muted-foreground">
                                {entry.message}
                            </p>
                            <span className="shrink-0 text-sm text-muted-foreground">
                                {entry.relativeTime}
                            </span>
                        </li>
                    ))}
                </ul>
            </CardContent>
        </Card>
    );
}
