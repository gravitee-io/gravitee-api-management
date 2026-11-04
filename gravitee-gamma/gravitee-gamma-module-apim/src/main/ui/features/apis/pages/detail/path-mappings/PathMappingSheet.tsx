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
import {
    Badge,
    Button,
    Checkbox,
    cn,
    Input,
    Label,
    RadioGroup,
    RadioGroupItem,
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
} from '@gravitee/graphene-core';
import { FileTextIcon, WaypointsIcon } from '@gravitee/graphene-core/icons';
import { useId, useState, type ComponentType, type FormEvent } from 'react';

import { useApiSwaggerPagePaths, useApiSwaggerPages } from '../../../hooks/useApiSwaggerPages';
import type { ApiSwaggerDocumentationPage } from '../../../types';
import { pathMappingValidationError } from '../../../utils/pathMappings';

const SOURCES = [
    {
        id: 'path',
        label: 'Type a path',
        description: 'Add one mapping, such as /products/:productId.',
        icon: WaypointsIcon,
    },
    {
        id: 'swagger',
        label: 'Import from Swagger',
        description: 'Add every path described on a Swagger document for this API.',
        icon: FileTextIcon,
    },
] as const;

type SourceId = (typeof SOURCES)[number]['id'];

export interface PathMappingSheetProps {
    open: boolean;
    mode: 'add' | 'edit';
    existing: string[];
    initialPath?: string;
    isPending?: boolean;
    environmentId?: string;
    apiId?: string;
    onOpenChange: (open: boolean) => void;
    onSubmit: (path: string) => void;
    onImport?: (pageId: string, overwrite: boolean) => void;
}

export function PathMappingSheet({
    open,
    mode,
    existing,
    initialPath = '',
    isPending = false,
    environmentId,
    apiId,
    onOpenChange,
    onSubmit,
    onImport,
}: Readonly<PathMappingSheetProps>) {
    const inputId = useId();
    const errorId = `${inputId}-error`;
    const [value, setValue] = useState(initialPath);
    const [touched, setTouched] = useState(false);
    const [source, setSource] = useState<SourceId>('path');
    const [selectedPageId, setSelectedPageId] = useState('');
    const [overwrite, setOverwrite] = useState(false);
    const [wasOpen, setWasOpen] = useState(open);
    if (open !== wasOpen) {
        setWasOpen(open);
        if (open) {
            setValue(initialPath);
            setTouched(false);
            setSource('path');
            setSelectedPageId('');
            setOverwrite(false);
        }
    }

    const swaggerImportEnabled = mode === 'add' && onImport !== undefined && !!environmentId && !!apiId;
    const swaggerQuery = useApiSwaggerPages(environmentId, apiId, open && swaggerImportEnabled && source === 'swagger');
    const swaggerPages = swaggerQuery.data ?? [];
    const selectedPathsQuery = useApiSwaggerPagePaths(
        environmentId,
        apiId,
        selectedPageId,
        open && swaggerImportEnabled && source === 'swagger' && selectedPageId.length > 0,
    );

    const current = mode === 'edit' ? initialPath : undefined;
    const error = pathMappingValidationError(value, existing, current);
    const showError = touched && error !== null;
    const pathCanSubmit = error === null && !isPending && (mode === 'add' || value.trim() !== initialPath);
    const swaggerCanSubmit = !!selectedPageId && !isPending && !swaggerQuery.isLoading;
    const importing = source === 'swagger' && swaggerImportEnabled;
    const canSubmit = importing ? swaggerCanSubmit : pathCanSubmit;

    function handleSubmit(event: FormEvent) {
        event.preventDefault();
        if (isPending) return;
        if (importing) {
            if (!selectedPageId || !onImport) return;
            onImport(selectedPageId, overwrite);
            return;
        }
        setTouched(true);
        if (error !== null) return;
        onSubmit(value.trim());
    }

    const title = mode === 'add' ? 'Add path mapping' : 'Edit path mapping';
    const description =
        mode === 'add'
            ? 'Choose how to add mappings. They apply the next time you deploy this API.'
            : 'The change applies the next time you deploy this API.';
    const submitLabel = importing ? 'Import' : mode === 'add' ? 'Add' : 'Save';

    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent side="right" style={{ maxWidth: '36rem' }}>
                <form onSubmit={handleSubmit} className="flex h-full flex-col">
                    <SheetHeader>
                        <SheetTitle>{title}</SheetTitle>
                        <SheetDescription>{description}</SheetDescription>
                    </SheetHeader>
                    <div className="flex-1 space-y-4 overflow-y-auto px-4">
                        {swaggerImportEnabled ? <SourceCards source={source} disabled={isPending} onChange={setSource} /> : null}
                        {importing ? (
                            <SwaggerDocumentPicker
                                pages={swaggerPages}
                                isLoading={swaggerQuery.isLoading}
                                isError={swaggerQuery.isError}
                                selectedPageId={selectedPageId}
                                selectedPaths={selectedPathsQuery.data}
                                selectedPathsLoading={selectedPathsQuery.isLoading}
                                selectedPathsError={selectedPathsQuery.isError}
                                overwrite={overwrite}
                                onSelect={setSelectedPageId}
                                onOverwriteChange={setOverwrite}
                            />
                        ) : (
                            <PathField
                                inputId={inputId}
                                errorId={errorId}
                                value={value}
                                showError={showError}
                                error={error}
                                onBlur={() => setTouched(true)}
                                onChange={setValue}
                            />
                        )}
                    </div>
                    <SheetFooter className="flex-row justify-end border-t">
                        <Button type="button" variant="outline" disabled={isPending} onClick={() => onOpenChange(false)}>
                            Cancel
                        </Button>
                        <Button type="submit" disabled={!canSubmit} data-testid="path-mapping-submit">
                            {submitLabel}
                        </Button>
                    </SheetFooter>
                </form>
            </SheetContent>
        </Sheet>
    );
}

function SourceCards({
    source,
    disabled,
    onChange,
}: Readonly<{ source: SourceId; disabled: boolean; onChange: (source: SourceId) => void }>) {
    return (
        <div className="grid gap-2" role="radiogroup" aria-label="How to add path mappings">
            {SOURCES.map(option => (
                <SourceCard key={option.id} option={option} selected={source === option.id} disabled={disabled} onChange={onChange} />
            ))}
        </div>
    );
}

function SourceCard({
    option,
    selected,
    disabled,
    onChange,
}: Readonly<{
    option: (typeof SOURCES)[number];
    selected: boolean;
    disabled: boolean;
    onChange: (source: SourceId) => void;
}>) {
    const Icon = option.icon;
    return (
        <button
            type="button"
            role="radio"
            aria-checked={selected}
            aria-label={option.label}
            disabled={disabled}
            onClick={() => onChange(option.id)}
            className={cn(
                'flex items-start gap-3 rounded-lg border p-3 text-left transition-colors disabled:cursor-not-allowed disabled:opacity-50',
                selected ? 'border-primary bg-primary/5' : 'border-border hover:bg-muted/50',
            )}
        >
            <SourceIcon icon={Icon} selected={selected} />
            <span className="min-w-0">
                <span className={cn('block text-sm font-medium', selected && 'text-primary')}>{option.label}</span>
                <span className="block text-xs text-muted-foreground">{option.description}</span>
            </span>
        </button>
    );
}

function SourceIcon({ icon: Icon, selected }: Readonly<{ icon: ComponentType<{ className?: string }>; selected: boolean }>) {
    return (
        <span
            className={cn(
                'flex size-8 shrink-0 items-center justify-center rounded-md',
                selected ? 'bg-primary/10 text-primary' : 'bg-muted text-muted-foreground',
            )}
        >
            <Icon className="size-4" aria-hidden />
        </span>
    );
}

function PathField({
    inputId,
    errorId,
    value,
    showError,
    error,
    onBlur,
    onChange,
}: Readonly<{
    inputId: string;
    errorId: string;
    value: string;
    showError: boolean;
    error: string | null;
    onBlur: () => void;
    onChange: (value: string) => void;
}>) {
    return (
        <div className="space-y-2">
            <Label htmlFor={inputId}>Path mapping</Label>
            <Input
                id={inputId}
                name="path"
                value={value}
                placeholder="/products/:productId"
                aria-invalid={showError}
                aria-describedby={showError ? errorId : undefined}
                data-testid="path-mapping-input"
                onBlur={onBlur}
                onChange={event => onChange(event.target.value)}
            />
            {showError ? (
                <p id={errorId} className="text-sm text-destructive" data-testid="path-mapping-error">
                    {error}
                </p>
            ) : null}
        </div>
    );
}

function importSummary(count: number, overwrite: boolean): string {
    const effect = overwrite
        ? "replaces the existing path mappings with the selected document's paths"
        : "adds the selected document's paths and keeps existing mappings";
    return `${count} on this API. Importing ${effect}.`;
}

function SwaggerDocumentPicker({
    pages,
    isLoading,
    isError,
    selectedPageId,
    selectedPaths,
    selectedPathsLoading,
    selectedPathsError,
    overwrite,
    onSelect,
    onOverwriteChange,
}: Readonly<{
    pages: ApiSwaggerDocumentationPage[];
    isLoading: boolean;
    isError: boolean;
    selectedPageId: string;
    selectedPaths: string[] | undefined;
    selectedPathsLoading: boolean;
    selectedPathsError: boolean;
    overwrite: boolean;
    onSelect: (pageId: string) => void;
    onOverwriteChange: (overwrite: boolean) => void;
}>) {
    const overwriteId = useId();
    if (isLoading) {
        return (
            <p className="text-sm text-muted-foreground" data-testid="swagger-pages-status">
                Loading Swagger pages...
            </p>
        );
    }
    if (isError) {
        return (
            <p className="text-sm text-destructive" role="alert" data-testid="swagger-pages-status">
                Swagger pages could not be loaded.
            </p>
        );
    }
    if (pages.length === 0) {
        return (
            <p className="text-sm text-muted-foreground" data-testid="swagger-pages-status">
                No swagger documentation on your API.
            </p>
        );
    }
    return (
        <div className="space-y-3">
            <div className="space-y-1">
                <p className="text-sm font-medium">Choose a document</p>
                <p className="text-sm text-muted-foreground">{importSummary(pages.length, overwrite)}</p>
            </div>
            <RadioGroup value={selectedPageId} onValueChange={onSelect} className="gap-2" aria-label="Swagger documents">
                {pages.map(page => (
                    <SwaggerDocumentOption
                        key={page.id}
                        page={page}
                        selected={selectedPageId === page.id}
                        paths={selectedPageId === page.id ? selectedPaths : undefined}
                        pathsLoading={selectedPageId === page.id && selectedPathsLoading}
                        pathsError={selectedPageId === page.id && selectedPathsError}
                    />
                ))}
            </RadioGroup>
            <label className="flex items-start gap-2" htmlFor={overwriteId}>
                <Checkbox
                    id={overwriteId}
                    checked={overwrite}
                    onCheckedChange={checked => onOverwriteChange(checked === true)}
                    data-testid="replace-existing-path-mappings"
                    className="mt-0.5"
                />
                <span className="text-sm">Replace existing path mappings</span>
            </label>
        </div>
    );
}

function SwaggerDocumentOption({
    page,
    selected,
    paths,
    pathsLoading,
    pathsError,
}: Readonly<{
    page: ApiSwaggerDocumentationPage;
    selected: boolean;
    paths: string[] | undefined;
    pathsLoading: boolean;
    pathsError: boolean;
}>) {
    const radioId = `swagger-page-${page.id}`;
    const extra = Math.max(0, (paths?.length ?? 0) - 1);
    return (
        <label
            htmlFor={radioId}
            className={cn(
                'flex cursor-pointer items-start gap-3 rounded-lg border p-3',
                selected ? 'border-primary bg-primary/5' : 'border-border',
            )}
        >
            <RadioGroupItem value={page.id} id={radioId} data-testid={radioId} className="mt-0.5" />
            <span className="min-w-0 space-y-1">
                <span className="block text-sm font-medium">{page.name}</span>
                {pathsLoading ? <span className="block text-xs text-muted-foreground">Loading paths...</span> : null}
                {pathsError ? <span className="block text-xs text-destructive">Paths could not be loaded.</span> : null}
                {paths?.[0] ? (
                    <span className="flex items-center gap-2 text-xs text-muted-foreground">
                        <span className="font-mono">{paths[0]}</span>
                        {extra > 0 ? <Badge variant="secondary">+{extra}</Badge> : null}
                    </span>
                ) : null}
            </span>
        </label>
    );
}
