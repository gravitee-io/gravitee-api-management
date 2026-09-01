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

import { Accordion, AccordionContent, AccordionItem, AccordionTrigger, Badge, Button, Skeleton } from '@gravitee/graphene-core';
import {
    AppWindowIcon,
    BellIcon,
    BoxesIcon,
    ChevronRightIcon,
    FileTextIcon,
    FolderOpenIcon,
    LayoutDashboardIcon,
    MonitorIcon,
    type LucideIcon,
} from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import type { NotificationTemplateCategory, NotificationTemplateListRow } from '../types/notificationTemplate';

const SCOPE_ICONS: Record<string, LucideIcon> = {
    API: LayoutDashboardIcon,
    API_PRODUCT: BoxesIcon,
    APPLICATION: AppWindowIcon,
    PORTAL: MonitorIcon,
    TEMPLATES_FOR_ACTION: FileTextIcon,
    TEMPLATES_FOR_ALERT: BellIcon,
    TEMPLATES_TO_INCLUDE: FolderOpenIcon,
};

function TemplateRow({ row }: Readonly<{ row: NotificationTemplateListRow }>) {
    const navigate = useNavigate();
    return (
        <button
            type="button"
            className="flex w-full items-center gap-3 border-b px-4 py-3 text-left last:border-b-0 hover:bg-muted"
            onClick={() => navigate(`${row.scope}/${encodeURIComponent(row.templateSegment)}`)}
        >
            <span className="min-w-0 flex-1 space-y-1">
                <span className="flex flex-wrap items-center gap-2">
                    <span className="text-sm font-medium">{row.name}</span>
                    {row.overridden ? (
                        <Badge variant="highlight" className="h-5 px-1.5 text-xs">
                            Custom
                        </Badge>
                    ) : null}
                </span>
                {row.description ? <span className="block text-xs text-muted-foreground">{row.description}</span> : null}
            </span>
            <ChevronRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
        </button>
    );
}

export function NotificationTemplatesList({
    categories,
    isLoading,
    templateCount,
    customCount,
}: Readonly<{
    categories: readonly NotificationTemplateCategory[];
    isLoading: boolean;
    templateCount: number;
    customCount: number;
}>) {
    const allScopes = useMemo(() => categories.map(category => category.scope), [categories]);
    const [openScopes, setOpenScopes] = useState<string[] | null>(null);
    const accordionValue = openScopes ?? allScopes;

    const allOpen = allScopes.length > 0 && allScopes.every(scope => accordionValue.includes(scope));

    if (isLoading) {
        return (
            <div className="space-y-2">
                {Array.from({ length: 7 }).map((_, index) => (
                    <Skeleton key={index} className="h-14 w-full rounded-xl" />
                ))}
            </div>
        );
    }

    if (categories.length === 0) {
        return <p className="text-sm text-muted-foreground">This organization has no notification templates configured.</p>;
    }

    return (
        <div className="space-y-3">
            <div className="flex flex-wrap items-center justify-end gap-3">
                <p className="text-sm text-muted-foreground">
                    {templateCount} templates
                    {customCount > 0 ? ` · ${customCount} custom` : null}
                </p>
                <Button type="button" variant="outline" size="sm" onClick={() => setOpenScopes(allOpen ? [] : [...allScopes])}>
                    {allOpen ? 'Collapse all' : 'Expand all'}
                </Button>
            </div>
            <div className="overflow-hidden rounded-xl border bg-card">
                <Accordion type="multiple" value={accordionValue} onValueChange={setOpenScopes}>
                    {categories.map(category => {
                        const Icon = SCOPE_ICONS[category.scope];
                        return (
                            <AccordionItem key={category.scope} value={category.scope} className="border-b last:border-b-0">
                                <AccordionTrigger className="px-4 py-3">
                                    <div className="flex min-w-0 flex-1 items-center gap-3">
                                        {Icon ? (
                                            <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary/5 text-primary">
                                                <Icon className="size-4" aria-hidden />
                                            </div>
                                        ) : null}
                                        <div className="min-w-0 space-y-1">
                                            <div className="flex flex-wrap items-center gap-2">
                                                <span className="text-sm font-semibold">{category.label}</span>
                                                <Badge variant="secondary" className="h-5 px-1.5 text-xs font-normal">
                                                    {category.rows.length}
                                                </Badge>
                                                {category.customCount > 0 ? (
                                                    <Badge variant="highlight" className="h-5 px-1.5 text-xs font-normal">
                                                        {category.customCount} custom
                                                    </Badge>
                                                ) : null}
                                            </div>
                                            <span className="text-xs text-muted-foreground">{category.description}</span>
                                        </div>
                                    </div>
                                </AccordionTrigger>
                                <AccordionContent className="pb-0">
                                    <div className="border-t">
                                        {category.rows.map(row => (
                                            <TemplateRow key={`${row.scope}-${row.name}`} row={row} />
                                        ))}
                                    </div>
                                </AccordionContent>
                            </AccordionItem>
                        );
                    })}
                </Accordion>
            </div>
        </div>
    );
}
