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
import { Button, Card, CardContent, CardHeader, CardTitle } from '@gravitee/graphene-core';
import { ChevronLeftIcon, ChevronRightIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useState } from 'react';

interface CarouselItem {
    readonly title: string;
    readonly description: string;
    readonly backgroundColor: string;
}

interface MediaItem {
    readonly message: string;
    readonly imageSrc: string;
    readonly imageAlt: string;
    readonly buttonText: string;
    readonly buttonUrl: string;
    readonly backgroundColor: string;
}

const CAROUSEL_ITEMS: readonly CarouselItem[] = [
    {
        title: 'API Management on Gravitee Cloud',
        description:
            'Design, secure, and scale your APIs with a fully managed gateway. Deploy in minutes and focus on building great developer experiences.',
        backgroundColor: '#f7f8fd',
    },
    {
        title: 'Hybrid and SaaS deployment options',
        description:
            'Run gateways in Gravitee-managed regions or connect hybrid gateways in your own infrastructure while keeping centralized control.',
        backgroundColor: '#e8f4f8',
    },
    {
        title: 'Unified account administration',
        description:
            'Manage organizations, environments, members, and access from a single Cloud console — now integrated into Gamma Console.',
        backgroundColor: '#f0f7ff',
    },
];

const MEDIA_ITEM: MediaItem = {
    message: 'Explore the Gravitee API Designer',
    imageSrc: 'https://www.gravitee.io/hubfs/Gravitee-Cloud-assets/api_designer_large.png',
    imageAlt: 'Gravitee API Designer',
    buttonText: 'Learn more',
    buttonUrl: 'https://www.gravitee.io/platform/api-designer',
    backgroundColor: '#f7f8fd',
};

export function CloudLearnAboutSection() {
    const [index, setIndex] = useState(0);
    const [expanded, setExpanded] = useState(true);
    const item = CAROUSEL_ITEMS[index]!;

    const prev = useCallback(() => {
        setIndex(i => (i === 0 ? CAROUSEL_ITEMS.length - 1 : i - 1));
    }, []);

    const next = useCallback(() => {
        setIndex(i => (i === CAROUSEL_ITEMS.length - 1 ? 0 : i + 1));
    }, []);

    if (!expanded) {
        return (
            <Card data-testid="cloud-home-carousel">
                <CardHeader className="cursor-pointer py-4" onClick={() => setExpanded(true)}>
                    <CardTitle className="text-base" data-testid="learning-content-tile-title">
                        Learn about Gravitee
                    </CardTitle>
                </CardHeader>
            </Card>
        );
    }

    return (
        <Card data-testid="cloud-home-carousel">
            <CardHeader className="pb-2">
                <CardTitle className="text-base" data-testid="learning-content-tile-title">
                    Learn about Gravitee
                </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
                <div className="grid gap-4 lg:grid-cols-2">
                    <div
                        className="relative flex min-h-48 flex-col justify-between rounded-lg p-6"
                        style={{ backgroundColor: item.backgroundColor }}
                    >
                        <div className="space-y-2 pr-8">
                            <h4 className="text-base font-semibold">{item.title}</h4>
                            <p className="text-sm text-muted-foreground">{item.description}</p>
                        </div>
                        <div className="mt-4 flex items-center justify-between">
                            <div className="flex gap-1">
                                {CAROUSEL_ITEMS.map((_, i) => (
                                    <button
                                        key={i}
                                        type="button"
                                        aria-label={`Go to slide ${i + 1}`}
                                        className={`size-2 rounded-full ${i === index ? 'bg-primary' : 'bg-muted-foreground/30'}`}
                                        onClick={() => setIndex(i)}
                                    />
                                ))}
                            </div>
                            <div className="flex gap-1">
                                <Button variant="outline" size="icon-sm" onClick={prev} aria-label="Previous slide">
                                    <ChevronLeftIcon aria-hidden />
                                </Button>
                                <Button variant="outline" size="icon-sm" onClick={next} aria-label="Next slide">
                                    <ChevronRightIcon aria-hidden />
                                </Button>
                            </div>
                        </div>
                    </div>
                    <div
                        className="flex min-h-48 flex-col justify-between gap-4 rounded-lg p-6 sm:flex-row sm:items-center"
                        style={{ backgroundColor: MEDIA_ITEM.backgroundColor }}
                    >
                        <div className="space-y-3">
                            <p className="text-base font-medium">{MEDIA_ITEM.message}</p>
                            <Button asChild>
                                <a href={MEDIA_ITEM.buttonUrl} target="_blank" rel="noopener noreferrer">
                                    {MEDIA_ITEM.buttonText}
                                </a>
                            </Button>
                        </div>
                        <img
                            src={MEDIA_ITEM.imageSrc}
                            alt={MEDIA_ITEM.imageAlt}
                            className="max-h-32 w-auto object-contain"
                            loading="lazy"
                        />
                    </div>
                </div>
            </CardContent>
        </Card>
    );
}
