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
    Button,
    Card,
    CardContent,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Field,
    FieldLabel,
    Input,
} from '@gravitee/graphene-core';
import { PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useEffect, useState } from 'react';

import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { notify } from '../../../shared/notify/notify';
import type { PageContentType } from '../../portals/types';
import { usePageTemplates } from '../hooks/usePageTemplates';
import {
    MODULE_CONFIG_SECTION_META,
    PAGE_TEMPLATE_CONTENT_TYPE_LABELS,
    type PageTemplate,
} from '../types';

const CONTENT_TYPES: readonly PageContentType[] = ['BLOCK', 'OPENAPI', 'HTML', 'ASYNCAPI'];

export function TemplatesPage() {
    const { templates, loading, addTemplate, updateTemplate, removeTemplate } = usePageTemplates();

    const [createOpen, setCreateOpen] = useState(false);
    const [previewTemplate, setPreviewTemplate] = useState<PageTemplate | null>(null);
    const [editTemplate, setEditTemplate] = useState<PageTemplate | null>(null);
    const [templateToDelete, setTemplateToDelete] = useState<PageTemplate | null>(null);
    const [isDeleting, setIsDeleting] = useState(false);

    if (loading) {
        return <p className="p-6 text-sm text-muted-foreground">Loading templates…</p>;
    }

    const meta = MODULE_CONFIG_SECTION_META.templates;

    return (
        <div className="mx-auto max-w-screen-2xl space-y-6 p-6">
            <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-bold tracking-tight">{meta.title}</h1>
                    <p className="text-sm text-muted-foreground">{meta.description}</p>
                </div>
                <Button type="button" onClick={() => setCreateOpen(true)}>
                    <PlusIcon className="size-4" aria-hidden />
                    Create template
                </Button>
            </div>

            <Card>
                <CardContent className="p-0">
                    <div className="overflow-x-auto">
                        <table className="w-full min-w-[44rem] border-collapse text-left text-sm">
                            <caption className="sr-only">Page templates for navigation items</caption>
                            <thead className="border-b border-border/70 bg-muted/40 text-xs uppercase tracking-wide text-muted-foreground">
                                <tr>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Name
                                    </th>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Content type
                                    </th>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Source
                                    </th>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Actions
                                    </th>
                                </tr>
                            </thead>
                            <tbody>
                                {templates.map(template => (
                                    <tr key={template.id} className="border-b border-border/60 last:border-b-0">
                                        <td className="px-5 py-4 align-middle">
                                            <div className="font-medium">{template.name}</div>
                                            <div className="mt-0.5 max-w-md text-xs text-muted-foreground">
                                                {template.description || '—'}
                                            </div>
                                        </td>
                                        <td className="px-5 py-4 align-middle text-muted-foreground">
                                            {PAGE_TEMPLATE_CONTENT_TYPE_LABELS[template.contentType]}
                                        </td>
                                        <td className="px-5 py-4 align-middle">
                                            <span
                                                className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${
                                                    template.system
                                                        ? 'bg-muted text-muted-foreground'
                                                        : 'bg-primary/10 text-primary'
                                                }`}
                                            >
                                                {template.system ? 'System' : 'Custom'}
                                            </span>
                                        </td>
                                        <td className="px-5 py-4 align-middle">
                                            <div className="flex flex-wrap items-center gap-2">
                                                <Button
                                                    type="button"
                                                    variant="outline"
                                                    size="sm"
                                                    onClick={() => setPreviewTemplate(template)}
                                                >
                                                    Preview
                                                </Button>
                                                <Button
                                                    type="button"
                                                    variant="outline"
                                                    size="sm"
                                                    onClick={() => setEditTemplate(template)}
                                                >
                                                    Edit
                                                </Button>
                                                {!template.system ? (
                                                    <Button
                                                        type="button"
                                                        variant="ghost"
                                                        size="sm"
                                                        aria-label={`Delete ${template.name}`}
                                                        onClick={() => setTemplateToDelete(template)}
                                                    >
                                                        <Trash2Icon className="size-4" aria-hidden />
                                                    </Button>
                                                ) : null}
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </CardContent>
            </Card>

            <CreateTemplateDialog
                open={createOpen}
                onOpenChange={setCreateOpen}
                onCreate={input => {
                    void addTemplate(input).then(() => notify.success('Template created.'));
                }}
            />

            <EditTemplateDialog
                open={editTemplate !== null}
                template={editTemplate}
                onOpenChange={open => {
                    if (!open) {
                        setEditTemplate(null);
                    }
                }}
                onSave={(id, patch) => {
                    void updateTemplate(id, patch).then(() => {
                        notify.success('Template updated.');
                        setEditTemplate(null);
                    });
                }}
            />

            <Dialog
                open={previewTemplate !== null}
                onOpenChange={open => {
                    if (!open) {
                        setPreviewTemplate(null);
                    }
                }}
            >
                <DialogContent style={{ width: 'min(92vw, 36rem)' }}>
                    <DialogHeader>
                        <DialogTitle>{previewTemplate?.name ?? 'Preview'}</DialogTitle>
                        <DialogDescription>
                            Stub preview only — full page editor is not available here.
                        </DialogDescription>
                    </DialogHeader>
                    <pre className="max-h-80 overflow-auto rounded-md border border-border/70 bg-muted/30 p-4 text-xs whitespace-pre-wrap">
                        {previewTemplate?.bodyStub ?? ''}
                    </pre>
                    <DialogFooter>
                        <Button type="button" variant="outline" onClick={() => setPreviewTemplate(null)}>
                            Close
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            <ConfirmDialog
                open={templateToDelete !== null}
                onOpenChange={open => {
                    if (!open) {
                        setTemplateToDelete(null);
                    }
                }}
                title="Delete template?"
                description={
                    templateToDelete
                        ? `This will permanently delete "${templateToDelete.name}".`
                        : undefined
                }
                confirmLabel="Delete"
                pendingLabel="Deleting…"
                destructive
                isPending={isDeleting}
                onConfirm={() => {
                    if (!templateToDelete) {
                        return;
                    }
                    setIsDeleting(true);
                    void removeTemplate(templateToDelete.id)
                        .then(() => {
                            notify.success('Template deleted.');
                            setTemplateToDelete(null);
                        })
                        .finally(() => setIsDeleting(false));
                }}
            />
        </div>
    );
}

function CreateTemplateDialog({
    open,
    onOpenChange,
    onCreate,
}: {
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly onCreate: (input: {
        name: string;
        description: string;
        contentType: PageContentType;
        bodyStub: string;
    }) => void;
}) {
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [contentType, setContentType] = useState<PageContentType>('BLOCK');
    const [bodyStub, setBodyStub] = useState('');

    useEffect(() => {
        if (!open) {
            setName('');
            setDescription('');
            setContentType('BLOCK');
            setBodyStub('');
        }
    }, [open]);

    const canSubmit = name.trim().length > 0;

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent style={{ width: 'min(92vw, 32rem)' }}>
                <DialogHeader>
                    <DialogTitle>Create template</DialogTitle>
                    <DialogDescription>
                        Create a reusable page template stub. A full editor is not required for this POC.
                    </DialogDescription>
                </DialogHeader>
                <form
                    className="space-y-4 py-2"
                    onSubmit={event => {
                        event.preventDefault();
                        if (!canSubmit) {
                            return;
                        }
                        onCreate({
                            name: name.trim(),
                            description: description.trim(),
                            contentType,
                            bodyStub: bodyStub.trim(),
                        });
                        onOpenChange(false);
                    }}
                >
                    <Field>
                        <FieldLabel htmlFor="tpl-name">Name</FieldLabel>
                        <Input
                            id="tpl-name"
                            value={name}
                            onChange={event => setName(event.target.value)}
                            placeholder="e.g. Getting started"
                            autoFocus
                            required
                        />
                    </Field>
                    <Field>
                        <FieldLabel htmlFor="tpl-description">Description</FieldLabel>
                        <Input
                            id="tpl-description"
                            value={description}
                            onChange={event => setDescription(event.target.value)}
                            placeholder="Optional short description"
                        />
                    </Field>
                    <Field>
                        <FieldLabel htmlFor="tpl-content-type">Content type</FieldLabel>
                        <select
                            id="tpl-content-type"
                            value={contentType}
                            onChange={event => setContentType(event.target.value as PageContentType)}
                            className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                        >
                            {CONTENT_TYPES.map(type => (
                                <option key={type} value={type}>
                                    {PAGE_TEMPLATE_CONTENT_TYPE_LABELS[type]}
                                </option>
                            ))}
                        </select>
                    </Field>
                    <Field>
                        <FieldLabel htmlFor="tpl-body">Body stub</FieldLabel>
                        <textarea
                            id="tpl-body"
                            value={bodyStub}
                            onChange={event => setBodyStub(event.target.value)}
                            rows={5}
                            placeholder="Optional placeholder content"
                            className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                        />
                    </Field>
                    <DialogFooter>
                        <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                            Cancel
                        </Button>
                        <Button type="submit" disabled={!canSubmit}>
                            Create
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}

function EditTemplateDialog({
    open,
    template,
    onOpenChange,
    onSave,
}: {
    readonly open: boolean;
    readonly template: PageTemplate | null;
    readonly onOpenChange: (open: boolean) => void;
    readonly onSave: (id: string, patch: { name: string; description: string; bodyStub: string }) => void;
}) {
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [bodyStub, setBodyStub] = useState('');

    useEffect(() => {
        if (!open || !template) {
            return;
        }
        setName(template.name);
        setDescription(template.description);
        setBodyStub(template.bodyStub);
    }, [open, template]);

    const canSubmit = name.trim().length > 0 && template !== null;

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent style={{ width: 'min(92vw, 32rem)' }}>
                <DialogHeader>
                    <DialogTitle>Edit template</DialogTitle>
                    <DialogDescription>Update name, description, and stub content.</DialogDescription>
                </DialogHeader>
                <form
                    className="space-y-4 py-2"
                    onSubmit={event => {
                        event.preventDefault();
                        if (!canSubmit || !template) {
                            return;
                        }
                        onSave(template.id, {
                            name: name.trim(),
                            description: description.trim(),
                            bodyStub,
                        });
                    }}
                >
                    <Field>
                        <FieldLabel htmlFor="edit-tpl-name">Name</FieldLabel>
                        <Input
                            id="edit-tpl-name"
                            value={name}
                            onChange={event => setName(event.target.value)}
                            required
                        />
                    </Field>
                    <Field>
                        <FieldLabel htmlFor="edit-tpl-description">Description</FieldLabel>
                        <Input
                            id="edit-tpl-description"
                            value={description}
                            onChange={event => setDescription(event.target.value)}
                        />
                    </Field>
                    <Field>
                        <FieldLabel htmlFor="edit-tpl-body">Body stub</FieldLabel>
                        <textarea
                            id="edit-tpl-body"
                            value={bodyStub}
                            onChange={event => setBodyStub(event.target.value)}
                            rows={5}
                            className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                        />
                    </Field>
                    <DialogFooter>
                        <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                            Cancel
                        </Button>
                        <Button type="submit" disabled={!canSubmit}>
                            Save
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}
