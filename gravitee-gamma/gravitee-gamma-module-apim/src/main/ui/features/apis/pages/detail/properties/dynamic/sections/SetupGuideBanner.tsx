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
import { ChevronDownIcon, ChevronUpIcon, InfoIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

const EXAMPLE_UPSTREAM = `[
  { "name": "timeout", "value": "5000" },
  { "name": "retries",  "value": "3"    }
]`;

const EXAMPLE_JOLT = `[
  {
    "operation": "shift",
    "spec": {
      "*": {
        "name":  "[@0].key",
        "value": "[@0].value"
      }
    }
  }
]`;

export function SetupGuideBanner() {
    const [open, setOpen] = useState(false);

    return (
        <Card>
            <CardContent className="py-2.5 px-4">
                <button
                    type="button"
                    className="flex w-full items-center gap-2 text-sm font-medium text-left"
                    onClick={() => setOpen(o => !o)}
                    aria-expanded={open}
                >
                    <InfoIcon className="size-4 shrink-0 text-primary" aria-hidden />
                    <span className="flex-1">How dynamic properties work</span>
                    {open ? (
                        <ChevronUpIcon className="size-4 shrink-0" aria-hidden />
                    ) : (
                        <ChevronDownIcon className="size-4 shrink-0" aria-hidden />
                    )}
                </button>

                {open && (
                    <div className="mt-4 space-y-4 text-sm">
                        <ol className="space-y-3 text-sm text-muted-foreground">
                            <li className="flex gap-3">
                                <span className="flex size-5 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-semibold text-primary">
                                    1
                                </span>
                                <span>
                                    <strong className="text-foreground">Configure the HTTP request.</strong> The gateway polls your upstream
                                    endpoint on the defined schedule using the method, URL, and headers you specify here.
                                </span>
                            </li>
                            <li className="flex gap-3">
                                <span className="flex size-5 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-semibold text-primary">
                                    2
                                </span>
                                <span>
                                    <strong className="text-foreground">Write a JOLT transformation.</strong> The upstream response must be
                                    transformed into an array of{' '}
                                    <code className="font-mono bg-muted px-1 py-0.5 rounded text-xs">{'{"key":"k","value":"v"}'}</code>{' '}
                                    objects. Leave the default spec if the upstream already returns that shape.
                                </span>
                            </li>
                            <li className="flex gap-3">
                                <span className="flex size-5 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-semibold text-primary">
                                    3
                                </span>
                                <span>
                                    <strong className="text-foreground">Enable and save.</strong> Properties are refreshed automatically
                                    according to the cron schedule and become available in policies via{' '}
                                    <code className="font-mono bg-muted px-1 py-0.5 rounded text-xs">{"{#api.properties['key']}"}</code>.
                                </span>
                            </li>
                        </ol>

                        <div className="rounded-lg border bg-muted/40 p-4 space-y-3">
                            <p className="text-xs font-semibold text-foreground">Example</p>
                            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                                <div className="space-y-1">
                                    <p className="text-xs text-muted-foreground">Upstream response</p>
                                    <pre className="text-xs font-mono bg-background rounded border p-2 overflow-x-auto">
                                        {EXAMPLE_UPSTREAM}
                                    </pre>
                                </div>
                                <div className="space-y-1">
                                    <p className="text-xs text-muted-foreground">JOLT spec (shift example)</p>
                                    <pre className="text-xs font-mono bg-background rounded border p-2 overflow-x-auto">{EXAMPLE_JOLT}</pre>
                                </div>
                            </div>
                        </div>
                    </div>
                )}
            </CardContent>
        </Card>
    );
}
