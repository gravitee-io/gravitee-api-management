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
import { Alert, AlertDescription, AlertTitle, Button, Card, CardContent, cn } from '@gravitee/graphene-core';
import { ArrowRight, Plus, Radio, X } from 'lucide-react';
import { useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

import { navigateToNavKey, resolveModulePath } from '../config/routes';

export function DashboardPage() {
    const navigate = useNavigate();
    const location = useLocation();
    const { modulePrefix } = useMemo(() => resolveModulePath(location.pathname), [location.pathname]);

    const [showGettingStarted, setShowGettingStarted] = useState(true);

    return (
        <div className={cn('flex flex-col gap-6 p-6', ' ')}>
            <header className="space-y-2">
                <h1 className="font-semibold">API Management</h1>
                <p className="text-muted-foreground">
                    Manage1, secure, and monitor your REST, GraphQL, gRPC, and WebSocket APIs
                </p>
            </header>

            {showGettingStarted ? (
                <Alert className="rounded-lg p-4 flex items-start gap-3 bg-primary/10">
                    <button
                        type="button"
                        className="absolute right-3 top-3 rounded-sm p-1 text-muted-foreground hover:text-foreground"
                        aria-label="Dismiss getting started"
                        onClick={() => setShowGettingStarted(false)}
                    >
                        <X className="size-4" aria-hidden />
                    </button>

                    <div className="flex items-start gap-3">
                        <div className="size-9 rounded-lg flex items-center justify-center shrink-0 bg-primary/10">
                            <Radio className="text-primary size-4" aria-hidden />
                        </div>

                        <div>
                            <AlertTitle className="font-medium">Getting started with API Management</AlertTitle>
                            <AlertDescription className="text-muted-foreground text-xs">
                                APIs are the backbone of your platform. Create an API proxy to add authentication, rate limiting,
                                and observability to your upstream services, then publish to the developer portal.
                            </AlertDescription>

                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                className="mt-2"
                                onClick={() => navigateToNavKey(navigate, modulePrefix, 'apis')}
                            >
                                Create your first API
                                <ArrowRight aria-hidden />
                            </Button>
                        </div>
                    </div>
                </Alert>
            ) : null}

            <Card className="border-muted rounded-lg">
                <CardContent className="flex flex-col items-center justify-center gap-4 text-center">
                    <div className="bg-muted p-4 rounded-xl bg-primary/10">
                        <Radio className="text-orange-600 size-8" aria-hidden />
                    </div>

                    <div className="space-y-2">
                        <h2 className="text-base font-semibold">No APIs yet</h2>
                        <p className="text-muted-foreground max-w-md text-sm">
                            Create your first API proxy to secure and manage your upstream services. Import an OpenAPI spec or
                            build from scratch.
                        </p>
                    </div>

                    <div className="flex items-center gap-3">
                        <Button
                            type="button"
                            onClick={() => navigateToNavKey(navigate, modulePrefix, 'apis')}
                        >
                            <Plus aria-hidden />
                            Create API
                        </Button>
                        <Button
                            type="button"
                            variant="outline"
                            onClick={() => navigateToNavKey(navigate, modulePrefix, 'apis')}
                        >
                            Learn more
                        </Button>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}

