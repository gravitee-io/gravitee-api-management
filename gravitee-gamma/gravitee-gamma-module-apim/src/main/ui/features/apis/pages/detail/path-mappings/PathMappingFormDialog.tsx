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
    Button,
    Checkbox,
    Dialog,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
    Label,
    RadioGroup,
    RadioGroupItem,
} from '@gravitee/graphene-core';
import { useId, useState, type FormEvent } from 'react';

import { useApiSwaggerPages } from '../../../hooks/useApiSwaggerPages';
import { pathMappingValidationError } from '../../../utils/pathMappings';
import { SegmentedTabs } from '../general/SegmentedTabs';

const ADD_TABS = [
    { id: 'path', label: 'Path' },
    { id: 'swagger', label: 'Swagger Document' },
] as const;

type AddTabId = (typeof ADD_TABS)[number]['id'];

export interface PathMappingFormDialogProps {
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

export function PathMappingFormDialog({
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
}: Readonly<PathMappingFormDialogProps>) {
    const inputId = useId();
    const errorId = `${inputId}-error`;
    const overwriteId = `${inputId}-overwrite`;
    const [value, setValue] = useState(initialPath);
    const [touched, setTouched] = useState(false);
    const [activeTab, setActiveTab] = useState<AddTabId>('path');
    const [selectedPageId, setSelectedPageId] = useState('');
    const [overwrite, setOverwrite] = useState(false);
    const [wasOpen, setWasOpen] = useState(open);
    if (open !== wasOpen) {
        setWasOpen(open);
        if (open) {
            setValue(initialPath);
            setTouched(false);
            setActiveTab('path');
            setSelectedPageId('');
            setOverwrite(false);
        }
    }

    const swaggerImportEnabled = mode === 'add' && onImport !== undefined && !!environmentId && !!apiId;
    const swaggerQuery = useApiSwaggerPages(environmentId, apiId, open && swaggerImportEnabled && activeTab === 'swagger');
    const swaggerPages = swaggerQuery.data ?? [];

    const current = mode === 'edit' ? initialPath : undefined;
    const error = pathMappingValidationError(value, existing, current);
    const showError = touched && error !== null;
    const pathCanSubmit = error === null && !isPending && (mode === 'add' || value.trim() !== initialPath);
    const swaggerCanSubmit = !!selectedPageId && !isPending;
    const canSubmit = activeTab === 'swagger' && swaggerImportEnabled ? swaggerCanSubmit : pathCanSubmit;

    function handleSubmit(event: FormEvent) {
        event.preventDefault();
        if (isPending) return;
        if (activeTab === 'swagger' && swaggerImportEnabled) {
            if (!selectedPageId || !onImport) return;
            onImport(selectedPageId, overwrite);
            return;
        }
        setTouched(true);
        if (error !== null) return;
        onSubmit(value.trim());
    }

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent>
                <form onSubmit={handleSubmit}>
                    <DialogHeader>
                        <DialogTitle>{mode === 'add' ? 'Add path mapping' : 'Edit path mapping'}</DialogTitle>
                    </DialogHeader>
                    {swaggerImportEnabled ? (
                        <div className="pt-2">
                            <SegmentedTabs
                                tabs={ADD_TABS}
                                activeId={activeTab}
                                onChange={setActiveTab}
                                ariaLabel="Add path mapping source"
                            />
                        </div>
                    ) : null}
                    {activeTab === 'swagger' && swaggerImportEnabled ? (
                        <div className="space-y-4 py-4">
                            {swaggerQuery.isLoading ? (
                                <p className="text-sm text-muted-foreground" data-testid="swagger-pages-status">
                                    Loading Swagger pages...
                                </p>
                            ) : null}
                            {swaggerQuery.isError ? (
                                <p className="text-sm text-destructive" role="alert" data-testid="swagger-pages-status">
                                    Swagger pages could not be loaded.
                                </p>
                            ) : null}
                            {swaggerQuery.isSuccess && swaggerPages.length === 0 ? (
                                <p className="text-sm text-muted-foreground" data-testid="swagger-pages-status">
                                    No swagger documentation on your API.
                                </p>
                            ) : null}
                            {swaggerPages.length > 0 ? (
                                <RadioGroup
                                    value={selectedPageId}
                                    onValueChange={setSelectedPageId}
                                    className="gap-3"
                                    aria-label="Swagger pages"
                                >
                                    {swaggerPages.map(page => {
                                        const radioId = `${inputId}-page-${page.id}`;
                                        return (
                                            <div key={page.id} className="flex items-center gap-2">
                                                <RadioGroupItem value={page.id} id={radioId} data-testid={`swagger-page-${page.id}`} />
                                                <Label htmlFor={radioId} className="font-normal">
                                                    {page.name}
                                                </Label>
                                            </div>
                                        );
                                    })}
                                </RadioGroup>
                            ) : null}
                            <label className="flex items-start gap-2" htmlFor={overwriteId}>
                                <Checkbox
                                    id={overwriteId}
                                    checked={overwrite}
                                    onCheckedChange={checked => setOverwrite(checked === true)}
                                    data-testid="replace-existing-path-mappings"
                                    className="mt-0.5"
                                />
                                <span className="text-sm">Replace existing path mappings</span>
                            </label>
                        </div>
                    ) : (
                        <div className="space-y-2 py-4">
                            <Label htmlFor={inputId}>Path</Label>
                            <Input
                                id={inputId}
                                name="path"
                                value={value}
                                placeholder="/products/:productId"
                                aria-invalid={showError}
                                aria-describedby={showError ? errorId : undefined}
                                data-testid="path-mapping-input"
                                onBlur={() => setTouched(true)}
                                onChange={event => setValue(event.target.value)}
                            />
                            {showError ? (
                                <p id={errorId} className="text-sm text-destructive" data-testid="path-mapping-error">
                                    {error}
                                </p>
                            ) : (
                                <p className="text-sm text-muted-foreground">Must match /[a-zA-Z0-9:/._-]+</p>
                            )}
                        </div>
                    )}
                    <DialogFooter className="gap-2 sm:justify-end">
                        <Button type="button" variant="outline" disabled={isPending} onClick={() => onOpenChange(false)}>
                            Cancel
                        </Button>
                        <Button type="submit" disabled={!canSubmit} data-testid="path-mapping-submit">
                            {mode === 'add' ? 'Add path mapping' : 'Save'}
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}
