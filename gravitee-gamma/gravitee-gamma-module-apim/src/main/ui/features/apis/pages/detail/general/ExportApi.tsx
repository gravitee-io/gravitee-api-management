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
import { Button, Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from '@gravitee/graphene-core';
import { DownloadIcon, ExternalLinkIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import { CheckboxOptionList } from './CheckboxOptionList';
import { SegmentedTabs } from './SegmentedTabs';
import { EXPORT_INCLUDE_OPTIONS, type ExportIncludeKey } from '../../../utils/apiGeneralExport';

type ExportTab = 'gravitee' | 'crd' | 'terraform';

const TABS: { id: ExportTab; label: string }[] = [
    { id: 'gravitee', label: 'Gravitee API definition' },
    { id: 'crd', label: 'CRD API Definition' },
    { id: 'terraform', label: 'Terraform HCL resource' },
];

const DEFAULT_INCLUDE = Object.fromEntries(EXPORT_INCLUDE_OPTIONS.map(o => [o.id, true])) as Record<ExportIncludeKey, boolean>;

export function ExportApi({
    open,
    onOpenChange,
    onExport,
    isExporting,
    error,
}: Readonly<{
    open: boolean;
    onOpenChange: (v: boolean) => void;
    onExport: (tab: ExportTab, include: Record<ExportIncludeKey, boolean>) => void;
    isExporting?: boolean;
    error?: string | null;
}>) {
    const [tab, setTab] = useState<ExportTab>('gravitee');
    const [include, setInclude] = useState<Record<ExportIncludeKey, boolean>>(DEFAULT_INCLUDE);
    const [prevOpen, setPrevOpen] = useState(open);

    if (prevOpen !== open) {
        setPrevOpen(open);
        if (open) {
            setTab('gravitee');
            setInclude(DEFAULT_INCLUDE);
        }
    }

    const canExport = tab !== 'terraform' && !isExporting;

    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent side="right" style={{ maxWidth: '32rem' }}>
                <SheetHeader>
                    <SheetTitle>Export API</SheetTitle>
                    <SheetDescription>Download this API definition in the format that fits your workflow.</SheetDescription>
                </SheetHeader>

                <div className="flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto px-4">
                    <SegmentedTabs tabs={TABS} activeId={tab} onChange={setTab} ariaLabel="Export format" />

                    {tab === 'gravitee' && (
                        <div className="space-y-2">
                            <p className="text-sm font-medium">Include additional data</p>
                            <CheckboxOptionList
                                idPrefix="export"
                                options={EXPORT_INCLUDE_OPTIONS}
                                values={include}
                                onChange={(id, checked) => setInclude(prev => ({ ...prev, [id]: checked }))}
                            />
                        </div>
                    )}

                    {tab === 'crd' && (
                        <div className="rounded-md border bg-muted/30 p-4 text-sm text-muted-foreground">
                            Export your API Definition as a Kubernetes resource Definition and start using our Kubernetes Operator to manage
                            your API declaratively.{' '}
                            <a
                                href="https://documentation.gravitee.io/gravitee-kubernetes-operator-gko"
                                target="_blank"
                                rel="noopener noreferrer"
                                className="inline-flex items-center gap-1 text-primary hover:underline"
                            >
                                Link to documentation
                                <ExternalLinkIcon className="size-3.5" aria-hidden />
                            </a>
                        </div>
                    )}

                    {tab === 'terraform' && (
                        <div className="rounded-md border bg-muted/30 p-4 text-sm text-muted-foreground">
                            To know how to export your API Definition as a Terraform HCL file and start using the Gravitee Terraform
                            provider to manage your API declaratively follow this link:{' '}
                            <a
                                href="https://registry.terraform.io/providers/gravitee-io/apim/latest/docs/guides/docgen_export_tutorial"
                                target="_blank"
                                rel="noopener noreferrer"
                                className="inline-flex items-center gap-1 text-primary hover:underline"
                            >
                                HCL export tutorial
                                <ExternalLinkIcon className="size-3.5" aria-hidden />
                            </a>
                        </div>
                    )}

                    {error && <p className="text-sm text-destructive">{error}</p>}
                </div>

                <SheetFooter className="flex-row justify-end border-t">
                    <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                        Cancel
                    </Button>
                    {canExport && (
                        <Button type="button" disabled={isExporting} onClick={() => onExport(tab, include)}>
                            <DownloadIcon className="size-4" aria-hidden />
                            {isExporting ? 'Exporting…' : 'Export'}
                        </Button>
                    )}
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
