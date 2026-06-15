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
import { Button, Card, CardContent } from '@gravitee/graphene-core';
import { BrainCircuitIcon, KeyRoundIcon, PlusIcon, SparklesIcon, UsersRoundIcon } from '@gravitee/graphene-core/icons';

const STEPS = [
    {
        icon: BrainCircuitIcon,
        title: 'Bundle LLM proxies',
        body: 'Group governed LLM access — models, providers, and credentials stay behind the gateway.',
    },
    {
        icon: KeyRoundIcon,
        title: 'Set plans & budgets',
        body: 'Define API-key plans with request rate limits and per-subscription token budgets.',
    },
    {
        icon: UsersRoundIcon,
        title: 'Onboard consumers',
        body: 'Developers get their key, endpoint, and snippets in the Developer Portal — off they go.',
    },
];

export function AiProductsEmptyLanding({ onCreateProduct }: { onCreateProduct: () => void }) {
    return (
        <div className="flex flex-col items-center gap-8 py-12">
            <div className="flex flex-col items-center gap-3 text-center max-w-xl">
                <div className="rounded-xl border bg-primary/5 p-3">
                    <SparklesIcon className="size-7 text-primary" aria-hidden />
                </div>
                <h1 className="text-2xl font-semibold tracking-tight">AI Products</h1>
                <p className="text-sm text-muted-foreground">
                    One packaging layer for everything AI your platform exposes: bundle LLM proxies, set token budgets, and give every
                    developer governed access through a single key.
                </p>
                <Button onClick={onCreateProduct} className="mt-2">
                    <PlusIcon className="size-4" aria-hidden />
                    Create AI Product
                </Button>
            </div>

            <div className="flex gap-4 max-w-3xl">
                {STEPS.map(step => (
                    <Card key={step.title} className="flex-1">
                        <CardContent className="pt-6 space-y-2">
                            <step.icon className="size-5 text-primary" aria-hidden />
                            <p className="text-sm font-semibold">{step.title}</p>
                            <p className="text-xs leading-relaxed text-muted-foreground">{step.body}</p>
                        </CardContent>
                    </Card>
                ))}
            </div>
        </div>
    );
}
