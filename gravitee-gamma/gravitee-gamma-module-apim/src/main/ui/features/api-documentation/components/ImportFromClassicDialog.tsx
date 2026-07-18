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
import {
    Alert,
    AlertDescription,
    AlertTitle,
    Button,
    Checkbox,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Skeleton,
} from '@gravitee/graphene-core';
import { useEffect, useId, useMemo, useState } from 'react';

import { notify } from '@apim/portal-editor/shared/notify/notify';
import {
    CLASSIC_IMPORT_LIMITATIONS,
    countClassicPageNodes,
    DEFAULT_CLASSIC_IMPORT_MAPPING,
    getClassicDocumentationFixture,
    getDefaultSelectedPageIds,
    getImportablePageIds,
    type ClassicConflictMode,
    type ClassicImportMappingOptions,
    type ClassicPageNode,
} from '../mocks/classic-documentation.fixture';
import { ClassicPageTreePreview } from './ClassicPageTreePreview';
import styles from './ImportFromClassicDialog.module.scss';

const SCAN_DELAY_MS = 600;

interface ImportFromClassicDialogProps {
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly apiName: string;
}

export function ImportFromClassicDialog({ open, onOpenChange, apiName }: ImportFromClassicDialogProps) {
    const conflictModeId = useId();
    const [isScanning, setIsScanning] = useState(false);
    const [conflictMode, setConflictMode] = useState<ClassicConflictMode>('replace');
    const [mapping, setMapping] = useState<ClassicImportMappingOptions>(DEFAULT_CLASSIC_IMPORT_MAPPING);
    const [selectedPageIds, setSelectedPageIds] = useState<Set<string>>(() => new Set());

    const classicPages = useMemo(() => getClassicDocumentationFixture(apiName), [apiName]);
    const totalPageCount = useMemo(() => countClassicPageNodes(classicPages), [classicPages]);

    useEffect(() => {
        if (!open) {
            return;
        }

        setConflictMode('replace');
        setMapping(DEFAULT_CLASSIC_IMPORT_MAPPING);
        setSelectedPageIds(getDefaultSelectedPageIds(classicPages));
        setIsScanning(true);

        const timer = window.setTimeout(() => {
            setIsScanning(false);
        }, SCAN_DELAY_MS);

        return () => {
            window.clearTimeout(timer);
        };
    }, [classicPages, open]);

    useEffect(() => {
        if (!open) {
            return;
        }

        setSelectedPageIds(previous => {
            const importableIds = getImportablePageIds(classicPages, mapping.includeWarningPages);
            const next = new Set<string>();

            for (const id of importableIds) {
                if (previous.has(id)) {
                    next.add(id);
                }
            }

            if (next.size === 0) {
                return getDefaultSelectedPageIds(classicPages, mapping.includeWarningPages);
            }

            return next;
        });
    }, [classicPages, mapping.includeWarningPages, open]);

    const handleTogglePage = (pageId: string, selected: boolean) => {
        setSelectedPageIds(previous => {
            const next = new Set(previous);
            if (selected) {
                next.add(pageId);
            } else {
                next.delete(pageId);
            }
            return next;
        });
    };

    const handleSelectAllImportable = () => {
        setSelectedPageIds(new Set(getImportablePageIds(classicPages, mapping.includeWarningPages)));
    };

    const handleDeselectWarnings = () => {
        const warningIds = new Set(
            flattenWarningPageIds(classicPages),
        );
        setSelectedPageIds(previous => {
            const next = new Set(previous);
            warningIds.forEach(id => next.delete(id));
            return next;
        });
    };

    const handleMappingChange = <K extends keyof ClassicImportMappingOptions>(key: K, value: ClassicImportMappingOptions[K]) => {
        setMapping(previous => ({ ...previous, [key]: value }));
    };

    const handleImportMock = () => {
        notify.info('Import preview only — no changes were made to the draft.');
        onOpenChange(false);
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className={styles.content}>
                <DialogHeader>
                    <DialogTitle>Import from Classic APIM</DialogTitle>
                    <DialogDescription>
                        Bring documentation created in the Classic Console into this Gamma documentation draft.
                    </DialogDescription>
                </DialogHeader>

                <div className={styles.form}>
                    <section className={styles.section} aria-labelledby="classic-docs-found-heading">
                        <h3 id="classic-docs-found-heading" className={styles.sectionTitle}>
                            Classic documentation found
                        </h3>

                        {isScanning ? (
                            <div className={styles.scanning} data-testid="classic-import-scanning">
                                <Skeleton className="h-4 w-full" />
                                <Skeleton className="h-4 w-3/4" />
                                <Skeleton className="h-4 w-5/6" />
                                <p className={styles.scanningText}>
                                    Scanning Classic Console documentation for &ldquo;{apiName}&rdquo;&hellip;
                                </p>
                            </div>
                        ) : (
                            <>
                                <p className={styles.summary}>
                                    {totalPageCount} pages in Classic Console for this API
                                </p>

                                <ClassicPageTreePreview
                                    nodes={classicPages}
                                    selectedIds={selectedPageIds}
                                    includeWarningPages={mapping.includeWarningPages}
                                    onTogglePage={handleTogglePage}
                                />

                                <div className={styles.treeActions}>
                                    <Button type="button" variant="outline" size="sm" onClick={handleSelectAllImportable}>
                                        Select all importable
                                    </Button>
                                    <Button type="button" variant="outline" size="sm" onClick={handleDeselectWarnings}>
                                        Deselect warnings
                                    </Button>
                                </div>
                            </>
                        )}
                    </section>

                    {!isScanning ? (
                        <>
                            <section className={styles.section} aria-labelledby="import-options-heading">
                                <h3 id="import-options-heading" className={styles.sectionTitle}>
                                    Import options
                                </h3>
                                <div className={styles.field}>
                                    <Label htmlFor={conflictModeId}>Conflict handling</Label>
                                    <Select value={conflictMode} onValueChange={value => setConflictMode(value as ClassicConflictMode)}>
                                        <SelectTrigger id={conflictModeId}>
                                            <SelectValue />
                                        </SelectTrigger>
                                        <SelectContent position="popper" sideOffset={4} className={styles.selectContent}>
                                            <SelectItem value="replace">
                                                Replace current draft navigation (recommended for first import)
                                            </SelectItem>
                                            <SelectItem value="merge">
                                                Merge into current draft (keep existing pages, add imported)
                                            </SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>
                            </section>

                            <section className={styles.section} aria-labelledby="page-mapping-heading">
                                <h3 id="page-mapping-heading" className={styles.sectionTitle}>
                                    Page mapping
                                </h3>
                                <div className={styles.checkboxList}>
                                    <MappingCheckbox
                                        id="mapping-markdown"
                                        label="Markdown pages → BlockNote / GMD pages"
                                        checked={mapping.markdownToGmd}
                                        onCheckedChange={checked => handleMappingChange('markdownToGmd', checked === true)}
                                    />
                                    <MappingCheckbox
                                        id="mapping-swagger"
                                        label="OpenAPI / Swagger pages → composable API reference blocks"
                                        checked={mapping.swaggerToApiBlocks}
                                        onCheckedChange={checked => handleMappingChange('swaggerToApiBlocks', checked === true)}
                                    />
                                    <MappingCheckbox
                                        id="mapping-asyncapi"
                                        label="AsyncAPI pages → AsyncAPI page type"
                                        checked={mapping.asyncApiPages}
                                        onCheckedChange={checked => handleMappingChange('asyncApiPages', checked === true)}
                                    />
                                    <MappingCheckbox
                                        id="mapping-links"
                                        label="External links → LINK nav items"
                                        checked={mapping.externalLinks}
                                        onCheckedChange={checked => handleMappingChange('externalLinks', checked === true)}
                                    />
                                    <MappingCheckbox
                                        id="mapping-folders"
                                        label="Preserve folder hierarchy"
                                        checked={mapping.preserveFolders}
                                        onCheckedChange={checked => handleMappingChange('preserveFolders', checked === true)}
                                    />
                                    <MappingCheckbox
                                        id="mapping-homepage"
                                        label="Set imported homepage as Overview page"
                                        checked={mapping.setHomepageAsOverview}
                                        onCheckedChange={checked => handleMappingChange('setHomepageAsOverview', checked === true)}
                                    />
                                    <MappingCheckbox
                                        id="mapping-warnings"
                                        label="Include warning pages"
                                        checked={mapping.includeWarningPages}
                                        onCheckedChange={checked => handleMappingChange('includeWarningPages', checked === true)}
                                    />
                                </div>
                            </section>

                            <Alert variant="warning" data-testid="classic-import-limitations">
                                <AlertTitle>Limitations</AlertTitle>
                                <AlertDescription>
                                    <p className={styles.limitationsIntro}>
                                        The following Classic features are not fully supported in Gamma:
                                    </p>
                                    <ul className={styles.limitationsList}>
                                        {CLASSIC_IMPORT_LIMITATIONS.map(limitation => (
                                            <li key={limitation}>{limitation}</li>
                                        ))}
                                    </ul>
                                </AlertDescription>
                            </Alert>
                        </>
                    ) : null}
                </div>

                <DialogFooter className={styles.footer}>
                    <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                        Cancel
                    </Button>
                    <Button type="button" disabled={isScanning} onClick={handleImportMock}>
                        Import (mock)
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}

interface MappingCheckboxProps {
    readonly id: string;
    readonly label: string;
    readonly checked: boolean;
    readonly onCheckedChange: (checked: boolean) => void;
}

function MappingCheckbox({ id, label, checked, onCheckedChange }: MappingCheckboxProps) {
    return (
        <div className={styles.checkboxRow}>
            <Checkbox id={id} checked={checked} onCheckedChange={value => onCheckedChange(value === true)} />
            <Label htmlFor={id} className={styles.checkboxLabel}>
                {label}
            </Label>
        </div>
    );
}

function flattenWarningPageIds(nodes: readonly ClassicPageNode[]): string[] {
    const result: string[] = [];

    function walk(node: ClassicPageNode): void {
        if (node.status === 'warning' && node.type !== 'FOLDER') {
            result.push(node.id);
        }
        node.children?.forEach(walk);
    }

    nodes.forEach(walk);
    return result;
}
