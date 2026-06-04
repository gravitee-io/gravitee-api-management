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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import {
    Alert,
    AlertDescription,
    AlertTitle,
    Badge,
    Button,
    Card,
    Empty,
    EmptyDescription,
    EmptyHeader,
    EmptyTitle,
    Skeleton,
    Tabs,
    TabsContent,
    TabsList,
    TabsTrigger,
    cn,
} from '@gravitee/graphene-core';
import {
    BotIcon,
    BoxesIcon,
    BrainIcon,
    ChevronDownIcon,
    ChevronRightIcon,
    CircleIcon,
    FileTextIcon,
    GlobeIcon,
    NetworkIcon,
    RadioIcon,
    ServerIcon,
    ShieldIcon,
    UsersIcon,
    ZapIcon,
} from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useRef, useState, type ComponentType, type SVGProps } from 'react';
import { KpiTile } from '../../components/KpiTile';
import { MonacoEditor } from '../../components/MonacoEditor';
import { parseGaplSchema, type ParsedEntity } from '../../shared/gapl-parser';
import { useSchema } from '../../shared/hooks/useSchema';
import { CATEGORIES, getEntityCategoryId, type EntityCategoryId } from './entity-types';

type SchemaTab = 'code' | 'entities';
type IconType = ComponentType<SVGProps<SVGSVGElement>>;

const CATEGORY_ICONS = {
    principal: UsersIcon,
    mcp: ServerIcon,
    api: GlobeIcon,
    agent: BotIcon,
    model: BrainIcon,
    event: RadioIcon,
    custom: BoxesIcon,
} as const satisfies Record<EntityCategoryId, IconType>;

interface CategoryGroup {
    readonly id: EntityCategoryId;
    readonly label: string;
    readonly textColor: string;
    readonly entities: ParsedEntity[];
}

function groupByCategory(entities: readonly ParsedEntity[]): CategoryGroup[] {
    const byId = new Map<EntityCategoryId, ParsedEntity[]>();
    for (const entity of entities) {
        const id = getEntityCategoryId(entity.name) ?? 'custom';
        const bucket = byId.get(id);
        if (bucket) bucket.push(entity);
        else byId.set(id, [entity]);
    }
    return CATEGORIES.filter(c => byId.has(c.id)).map(c => ({ ...c, entities: byId.get(c.id) ?? [] }));
}

function parentLabel(entity: ParsedEntity): string | null {
    return entity.parents.length > 0 ? `in [${entity.parents.join(', ')}]` : null;
}

export function SchemaPage() {
    const env = useEnvironment();
    const environmentId = env?.id ?? '';
    const { schema, notFound, isLoading, error } = useSchema(environmentId);

    const schemaText = schema?.schemaText ?? '';
    const parsed = useMemo(() => parseGaplSchema(schemaText), [schemaText]);
    const groups = useMemo(() => groupByCategory(parsed.entities), [parsed.entities]);

    const principalKinds = useMemo(
        () => parsed.entities.filter(e => getEntityCategoryId(e.name) === 'principal').length,
        [parsed.entities],
    );
    const resourceKinds = useMemo(
        () =>
            parsed.entities.filter(e => {
                const category = getEntityCategoryId(e.name);
                return category !== undefined && category !== 'principal';
            }).length,
        [parsed.entities],
    );

    const [activeTab, setActiveTab] = useState<SchemaTab>('code');
    const [focused, setFocused] = useState<string | null>(null);
    const [collapsed, setCollapsed] = useState<ReadonlySet<EntityCategoryId>>(new Set());
    const cardRefs = useRef<Record<string, HTMLDivElement | null>>({});

    // Clicking an entity in the outline jumps to its card in the Entities tab.
    // The tab content mounts on switch, so scroll on the next frame once refs exist.
    useEffect(() => {
        if (activeTab !== 'entities' || !focused) return;
        const el = cardRefs.current[focused];
        if (!el) return;
        const raf = requestAnimationFrame(() => el.scrollIntoView({ behavior: 'smooth', block: 'center' }));
        return () => cancelAnimationFrame(raf);
    }, [activeTab, focused]);

    function selectEntity(name: string) {
        setFocused(name);
        setActiveTab('entities');
    }

    function toggleCategory(id: EntityCategoryId) {
        setCollapsed(prev => {
            const next = new Set(prev);
            if (next.has(id)) next.delete(id);
            else next.add(id);
            return next;
        });
    }

    const isEmpty = !isLoading && error === undefined && (notFound || schemaText.trim() === '');

    return (
        <div className="flex flex-col gap-4">
            <header className="flex items-start gap-3">
                <NetworkIcon className="mt-1 size-5 text-muted-foreground" aria-hidden />
                <div>
                    <h1 className="text-xl font-semibold">Schema</h1>
                    <p className="mt-1 max-w-3xl text-sm text-muted-foreground">
                        The entity types, their relationships, and the actions the policy engine can reason about — the contract your
                        policies are written against. This view is read-only.
                    </p>
                </div>
            </header>

            {error !== undefined && (
                <Alert variant="destructive">
                    <AlertTitle>Could not load schema</AlertTitle>
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            )}

            {isLoading && (
                <div className="flex flex-col gap-3">
                    <Skeleton className="h-8 w-1/3" />
                    <Skeleton className="h-10 w-full" />
                    <Skeleton className="h-10 w-full" />
                    <Skeleton className="h-10 w-full" />
                </div>
            )}

            {isEmpty && (
                <Empty>
                    <EmptyHeader>
                        <EmptyTitle>No schema defined yet</EmptyTitle>
                        <EmptyDescription>
                            Once a <span className="font-mono">schema.gapl</span> is published for this environment, its entity types and
                            actions will appear here.
                        </EmptyDescription>
                    </EmptyHeader>
                </Empty>
            )}

            {!isLoading && error === undefined && !isEmpty && (
                <>
                    {parsed.diagnostics.length > 0 && (
                        <Alert variant="destructive">
                            <AlertTitle>Schema could not be fully parsed</AlertTitle>
                            <AlertDescription>
                                <ul className="list-disc pl-4">
                                    {parsed.diagnostics.map((diagnostic, index) => (
                                        <li key={`${index}-${diagnostic}`} className="font-mono text-xs">
                                            {diagnostic}
                                        </li>
                                    ))}
                                </ul>
                            </AlertDescription>
                        </Alert>
                    )}

                    <div className="grid grid-cols-4 gap-4" aria-label="Schema summary">
                        <KpiTile
                            label="Entities"
                            value={parsed.entities.length}
                            Icon={BoxesIcon}
                            iconClassName="bg-primary/10 text-primary"
                        />
                        <KpiTile label="Actions" value={parsed.actions.length} Icon={ZapIcon} iconClassName="bg-warning/10 text-warning" />
                        <KpiTile
                            label="Principal kinds"
                            value={principalKinds}
                            Icon={UsersIcon}
                            iconClassName="bg-success/10 text-success"
                        />
                        <KpiTile
                            label="Resource kinds"
                            value={resourceKinds}
                            Icon={ShieldIcon}
                            iconClassName="bg-highlight/10 text-highlight"
                        />
                    </div>

                    <div className="flex flex-col gap-4 md:flex-row md:items-start">
                        <div className="shrink-0" style={{ width: 240, maxWidth: '100%' }}>
                            <Card className="w-full overflow-hidden p-0">
                                <div className="border-b px-3 py-2 text-xs font-medium uppercase tracking-wide text-muted-foreground">
                                    Outline
                                </div>
                                <div className="p-2">
                                    {groups.map(group => {
                                        const Icon = CATEGORY_ICONS[group.id];
                                        const isCollapsed = collapsed.has(group.id);
                                        return (
                                            <div key={group.id} className="mb-1 last:mb-0">
                                                <Button
                                                    variant="ghost"
                                                    size="sm"
                                                    onClick={() => toggleCategory(group.id)}
                                                    className="h-auto w-full justify-start gap-1.5 px-1 py-1"
                                                    aria-expanded={!isCollapsed}
                                                >
                                                    {isCollapsed ? (
                                                        <ChevronRightIcon className="size-3.5 shrink-0 text-muted-foreground" aria-hidden />
                                                    ) : (
                                                        <ChevronDownIcon className="size-3.5 shrink-0 text-muted-foreground" aria-hidden />
                                                    )}
                                                    <Icon className={cn('size-3.5 shrink-0', group.textColor)} aria-hidden />
                                                    <span className={cn('text-xs font-semibold uppercase tracking-wide', group.textColor)}>
                                                        {group.label}
                                                    </span>
                                                    <span className="ml-auto text-xs font-normal text-muted-foreground">
                                                        {group.entities.length}
                                                    </span>
                                                </Button>
                                                {!isCollapsed &&
                                                    group.entities.map(entity => (
                                                        <Button
                                                            key={entity.name}
                                                            variant="ghost"
                                                            size="sm"
                                                            onClick={() => selectEntity(entity.name)}
                                                            className={cn(
                                                                'h-auto w-full justify-start gap-2 py-1 pl-6 pr-2 font-normal',
                                                                focused === entity.name && 'bg-muted',
                                                            )}
                                                        >
                                                            <CircleIcon className="size-2 shrink-0 text-muted-foreground/50" aria-hidden />
                                                            <span className="truncate font-mono text-sm">{entity.name}</span>
                                                            {parentLabel(entity) && (
                                                                <span className="ml-auto shrink-0 font-mono text-xs text-muted-foreground">
                                                                    {parentLabel(entity)}
                                                                </span>
                                                            )}
                                                        </Button>
                                                    ))}
                                            </div>
                                        );
                                    })}
                                </div>
                            </Card>
                        </div>

                        <Tabs
                            value={activeTab}
                            onValueChange={value => {
                                if (value === 'code' || value === 'entities') setActiveTab(value);
                            }}
                            className="flex min-w-0 flex-1 flex-col gap-3"
                        >
                            <div className="flex items-center justify-between gap-2">
                                <TabsList variant="line">
                                    <TabsTrigger value="code">
                                        <FileTextIcon className="size-4" aria-hidden />
                                        schema.gapl
                                    </TabsTrigger>
                                    <TabsTrigger value="entities">
                                        <BoxesIcon className="size-4" aria-hidden />
                                        Entities
                                        <Badge variant="secondary">{parsed.entities.length}</Badge>
                                    </TabsTrigger>
                                </TabsList>
                                <span className="hidden shrink-0 font-mono text-xs text-muted-foreground sm:inline">{'</>'} GAPL</span>
                            </div>

                            <TabsContent value="code">
                                <div className="overflow-hidden rounded-lg border">
                                    <MonacoEditor value={schemaText} readOnly height={560} ariaLabel="Schema definition (read-only)" />
                                </div>
                            </TabsContent>

                            <TabsContent value="entities" className="flex flex-col gap-5">
                                {groups.map(group => {
                                    const Icon = CATEGORY_ICONS[group.id];
                                    return (
                                        <section key={group.id} className="flex flex-col gap-2">
                                            <h2 className={cn('flex items-center gap-1.5 text-sm font-semibold', group.textColor)}>
                                                <Icon className="size-4" aria-hidden />
                                                {group.label}
                                            </h2>
                                            <div className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3">
                                                {group.entities.map(entity => (
                                                    <Card
                                                        key={entity.name}
                                                        ref={el => {
                                                            cardRefs.current[entity.name] = el;
                                                        }}
                                                        className={cn(
                                                            'p-3 transition-shadow',
                                                            focused === entity.name && 'ring-2 ring-ring',
                                                        )}
                                                    >
                                                        <div className="flex flex-wrap items-center gap-2">
                                                            <span className="font-mono text-xs text-muted-foreground">{'{}'}</span>
                                                            <span className="font-mono text-sm font-medium">{entity.name}</span>
                                                            {parentLabel(entity) && (
                                                                <Badge variant="outline" className="font-mono text-xs">
                                                                    {parentLabel(entity)}
                                                                </Badge>
                                                            )}
                                                        </div>
                                                        {entity.attributes.length > 0 ? (
                                                            <div className="mt-2 flex flex-wrap gap-1.5">
                                                                {entity.attributes.map(attr => (
                                                                    <Badge
                                                                        key={attr.name}
                                                                        variant="secondary"
                                                                        className="font-mono text-xs"
                                                                    >
                                                                        {attr.name}: {attr.type}
                                                                    </Badge>
                                                                ))}
                                                            </div>
                                                        ) : (
                                                            <p className="mt-2 text-xs text-muted-foreground">No attributes.</p>
                                                        )}
                                                    </Card>
                                                ))}
                                            </div>
                                        </section>
                                    );
                                })}
                            </TabsContent>
                        </Tabs>
                    </div>
                </>
            )}
        </div>
    );
}
