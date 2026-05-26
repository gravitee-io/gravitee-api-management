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
import { Card, CardContent } from '@gravitee/graphene-core';
import { ArrowRightIcon, CircleCheckIcon, DatabaseIcon, GlobeIcon, ServerIcon } from '@gravitee/graphene-core/icons';

export function EndpointsLanding() {
    return (
        <Card>
            <CardContent className="p-6 space-y-5">
                <div>
                    <p className="text-sm font-semibold text-foreground">Why configure upstream endpoints?</p>
                    <p className="mt-1 text-sm text-muted-foreground">
                        Endpoints define where the gateway routes API requests. Each endpoint group points to one or more backend services
                        and controls how the gateway connects to them (load balancing, timeouts, SSL, proxy).
                    </p>
                </div>

                <div className="rounded-xl border border-primary/20 bg-primary/5 p-4">
                    <p className="text-xs font-medium text-primary mb-4">How it works</p>
                    <div className="flex items-center justify-center gap-4">
                        <div className="flex flex-col items-center gap-1.5">
                            <div className="size-10 rounded-xl border bg-card flex items-center justify-center">
                                <GlobeIcon className="size-4 text-muted-foreground" />
                            </div>
                            <span className="text-xs text-muted-foreground">Consumer</span>
                        </div>
                        <ArrowRightIcon className="size-4 text-muted-foreground shrink-0" />
                        <div className="flex flex-col items-center gap-1.5">
                            <div className="size-10 rounded-xl border bg-card flex items-center justify-center">
                                <ServerIcon className="size-4 text-muted-foreground" />
                            </div>
                            <span className="text-xs text-muted-foreground">Gateway</span>
                        </div>
                        <ArrowRightIcon className="size-4 text-muted-foreground shrink-0" />
                        <div className="flex flex-col items-center gap-1.5">
                            <div
                                className="size-10 rounded-xl border bg-card flex items-center justify-center"
                                style={{ borderWidth: '2px', borderColor: 'var(--color-primary)' }}
                            >
                                <DatabaseIcon className="size-4 text-primary" />
                            </div>
                            <span className="text-xs font-medium text-primary">Endpoint group</span>
                        </div>
                    </div>
                </div>

                <ul className="space-y-2">
                    {[
                        'Load balance traffic across multiple backend instances',
                        'Configure timeouts, keep-alive, and HTTP version per endpoint group',
                        'Add SSL/TLS settings and proxy configuration for secure communication',
                    ].map(item => (
                        <li key={item} className="flex items-start gap-2 text-sm text-muted-foreground">
                            <CircleCheckIcon className="size-4 text-success shrink-0 mt-0.5" />
                            {item}
                        </li>
                    ))}
                </ul>
            </CardContent>
        </Card>
    );
}
