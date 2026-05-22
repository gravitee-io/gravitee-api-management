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
import { Badge, Button, Card, CardContent } from '@gravitee/graphene-core';
import { ArchiveIcon, ArrowRightIcon, RadioIcon } from '@gravitee/graphene-core/icons';
import type { LucideIcon } from '@gravitee/graphene-core/icons';

interface GetStartedCardProps {
    Icon: LucideIcon;
    title: string;
    description: string;
    features: readonly string[];
    protocols: readonly string[];
    ctaLabel: string;
    onCta: () => void;
}

function GetStartedCard({ Icon, title, description, features, protocols, ctaLabel, onCta }: GetStartedCardProps) {
    return (
        <Card className="flex flex-col">
            <CardContent className="pt-6 pb-6 flex flex-col gap-5 h-full">
                <div className="rounded-xl bg-primary/10 p-3" style={{ width: 'fit-content' }}>
                    <Icon className="size-6 text-primary" aria-hidden />
                </div>

                <div>
                    <h3 className="text-base font-semibold">{title}</h3>
                    <p className="text-sm text-muted-foreground mt-1.5 leading-relaxed">{description}</p>
                </div>

                <ul className="space-y-2 flex-1">
                    {features.map(feature => (
                        <li key={feature} className="flex items-center gap-2 text-sm text-muted-foreground">
                            <span className="size-1.5 rounded-full bg-primary shrink-0" aria-hidden />
                            {feature}
                        </li>
                    ))}
                </ul>

                <div>
                    <p className="text-xs text-muted-foreground mb-2">Protocols:</p>
                    <div className="flex flex-wrap gap-1.5">
                        {protocols.map(p => (
                            <Badge key={p} variant="outline" className="text-xs">
                                {p}
                            </Badge>
                        ))}
                    </div>
                </div>

                <Button onClick={onCta} className="w-full">
                    {ctaLabel}
                    <ArrowRightIcon className="size-4" aria-hidden />
                </Button>
            </CardContent>
        </Card>
    );
}

// ─── Static card data ─────────────────────────────────────────────────────────

const API_PROXY_FEATURES = [
    'Authentication & authorization',
    'Rate limiting & quotas',
    'Request / response transformation',
    'Logging & analytics',
] as const;

const API_PROXY_PROTOCOLS = ['REST', 'GraphQL', 'gRPC', 'WebSocket'] as const;

const API_PRODUCT_FEATURES = ['Bundle multiple APIs into one surface', 'Subscription & plan management', 'Unified access control'] as const;

const API_PRODUCT_PROTOCOLS = ['HTTP', 'REST', 'GraphQL'] as const;

// ─── Public component ─────────────────────────────────────────────────────────

interface DashboardGetStartedCardsProps {
    onCreateProxy: () => void;
    onCreateProduct: () => void;
}

export function DashboardGetStartedCards({ onCreateProxy, onCreateProduct }: DashboardGetStartedCardsProps) {
    return (
        <div className="grid grid-cols-2 gap-4">
            <GetStartedCard
                Icon={RadioIcon}
                title="Expose APIs as HTTP Proxies"
                description="Secure and observe your backend services with a Gravitee proxy layer. Add authentication, rate limiting, and traffic management in front of any HTTP, GraphQL, gRPC, or WebSocket service."
                features={API_PROXY_FEATURES}
                protocols={API_PROXY_PROTOCOLS}
                ctaLabel="Create API Proxy"
                onCta={onCreateProxy}
            />
            <GetStartedCard
                Icon={ArchiveIcon}
                title="Bundle APIs into Products"
                description="Group multiple APIs into consumable products. Define plans, manage subscriptions, and give developers a unified surface to discover and access your APIs through the developer portal."
                features={API_PRODUCT_FEATURES}
                protocols={API_PRODUCT_PROTOCOLS}
                ctaLabel="Create API Product"
                onCta={onCreateProduct}
            />
        </div>
    );
}
