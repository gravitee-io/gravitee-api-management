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
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
    Label,
    Textarea,
} from '@gravitee/graphene-core';
import { BoxesIcon, CheckIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { SyncStatusBadge } from '../features/api-products/components/SyncStatusBadge';
import { useApiProductDetailContext } from '../features/api-products/context/ApiProductDetailContext';
import { useDeleteApiProduct } from '../features/api-products/hooks/useDeleteApiProduct';
import { useUpdateApiProduct } from '../features/api-products/hooks/useUpdateApiProduct';

function formatDate(iso?: string) {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('en-GB', { dateStyle: 'medium', timeStyle: 'short' });
}

export function ApiProductGeneralPage() {
    const { productId } = useParams<{ productId: string }>();
    const navigate = useNavigate();
    const { product, isLoading } = useApiProductDetailContext();

    const [name, setName] = useState('');
    const [version, setVersion] = useState('');
    const [description, setDescription] = useState('');
    const [deleteConfirm, setDeleteConfirm] = useState('');
    const [confirmAction, setConfirmAction] = useState<'remove-apis' | 'delete' | null>(null);

    useEffect(() => {
        if (product) {
            setName(product.name);
            setVersion(product.version);
            setDescription(product.description ?? '');
            setDeleteConfirm('');
        }
    }, [product]);

    const { mutate: updateProduct, isPending: isSaving, error: saveError } = useUpdateApiProduct(productId ?? '');
    const { mutate: deleteProduct, isPending: isDeleting } = useDeleteApiProduct();

    const isDirty = product && (name !== product.name || version !== product.version || description !== (product.description ?? ''));
    const canSave = isDirty && name.trim() && version.trim() && !isSaving;

    function handleSave(e: React.FormEvent) {
        e.preventDefault();
        if (!canSave || !product) return;
        updateProduct({
            name: name.trim(),
            version: version.trim(),
            description: description.trim() || undefined,
            apiIds: product.apiIds ?? [],
        });
    }

    function handleRemoveAllApis() {
        if (!product) return;
        updateProduct({ name: product.name, version: product.version, description: product.description, apiIds: [] });
        setConfirmAction(null);
    }

    function handleDelete() {
        if (!productId) return;
        deleteProduct(productId, { onSuccess: () => navigate('../..') });
        setConfirmAction(null);
    }

    if (isLoading)
        return (
            <div className="p-6">
                <p className="text-sm text-muted-foreground">Loading…</p>
            </div>
        );

    return (
        <div className="space-y-5 p-6">
            <div className="flex items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">General</h1>
                    <p className="text-sm text-muted-foreground">Manage name, version, description, and product lifecycle.</p>
                </div>
                <Button size="sm" onClick={handleSave} disabled={!canSave}>
                    <CheckIcon className="size-4" aria-hidden />
                    {isSaving ? 'Saving…' : 'Save changes'}
                </Button>
            </div>

            {/* Single card: form + internal details panel */}
            <form onSubmit={handleSave}>
                <Card>
                    <CardContent className="pt-6">
                        <div className="flex gap-8">
                            {/* Left: form fields */}
                            <div className="flex-1 min-w-0 space-y-4">
                                <div className="flex gap-4">
                                    <div className="flex-1 min-w-0 space-y-2">
                                        <Label htmlFor="gen-name">
                                            Name <span className="text-destructive">*</span>
                                        </Label>
                                        <Input
                                            id="gen-name"
                                            value={name}
                                            onChange={e => setName(e.target.value)}
                                            placeholder="Product name"
                                        />
                                    </div>
                                    <div className="space-y-2 w-32">
                                        <Label htmlFor="gen-version">
                                            Version <span className="text-destructive">*</span>
                                        </Label>
                                        <Input
                                            id="gen-version"
                                            value={version}
                                            onChange={e => setVersion(e.target.value)}
                                            placeholder="1.0.0"
                                            maxLength={32}
                                        />
                                    </div>
                                </div>

                                <div className="space-y-2">
                                    <Label htmlFor="gen-description">Description</Label>
                                    <Textarea
                                        id="gen-description"
                                        value={description}
                                        onChange={e => setDescription(e.target.value)}
                                        placeholder="Describe this product…"
                                        rows={4}
                                        style={{ fieldSizing: 'fixed' } as React.CSSProperties}
                                    />
                                </div>

                                {saveError ? (
                                    <p className="text-sm text-destructive rounded-md border border-destructive/20 bg-destructive/5 px-3 py-2">
                                        {saveError.message}
                                    </p>
                                ) : null}
                            </div>

                            {/* Right: details panel separated by left border */}
                            <div className="shrink-0 border-l pl-8 space-y-3" style={{ width: '17.5rem' }}>
                                <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">Details</p>
                                <dl className="space-y-2.5 text-sm">
                                    <div className="flex items-start justify-between gap-2">
                                        <dt className="text-muted-foreground shrink-0">Owner</dt>
                                        <dd className="font-medium text-right truncate max-w-40" title={product?.primaryOwner?.displayName}>
                                            {product?.primaryOwner?.displayName ?? 'You'}
                                        </dd>
                                    </div>
                                    <div className="flex items-start justify-between gap-2">
                                        <dt className="text-muted-foreground shrink-0">Created</dt>
                                        <dd className="text-right text-xs">{formatDate(product?.createdAt)}</dd>
                                    </div>
                                    <div className="flex items-start justify-between gap-2">
                                        <dt className="text-muted-foreground shrink-0">Updated</dt>
                                        <dd className="text-right text-xs">{formatDate(product?.updatedAt)}</dd>
                                    </div>
                                    <div className="flex items-start justify-between gap-2">
                                        <dt className="text-muted-foreground shrink-0">Gateway sync</dt>
                                        <dd>
                                            {product?.deploymentState ? (
                                                <SyncStatusBadge state={product.deploymentState} compact />
                                            ) : (
                                                <span className="text-xs">—</span>
                                            )}
                                        </dd>
                                    </div>
                                </dl>
                            </div>
                        </div>
                    </CardContent>
                </Card>
            </form>

            {/* API Product Events */}
            <Card>
                <CardHeader className="pb-3">
                    <CardTitle className="text-sm">API Product Events</CardTitle>
                    <CardDescription className="text-xs">
                        These actions change which APIs are bundled in this product, or remove the product entirely.
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <div className="grid grid-cols-2 gap-3">
                        {/* Remove all APIs */}
                        <button
                            type="button"
                            disabled={isSaving || !product?.apiIds?.length}
                            onClick={() => setConfirmAction('remove-apis')}
                            className="flex items-start gap-3 rounded-lg border p-3 text-left transition-colors hover:bg-accent/50 disabled:pointer-events-none disabled:opacity-50"
                        >
                            <div className="shrink-0 rounded-lg p-1.5" style={{ backgroundColor: 'hsl(var(--warning) / 0.1)' }}>
                                <BoxesIcon className="size-4" style={{ color: 'hsl(var(--warning))' }} aria-hidden />
                            </div>
                            <div>
                                <p className="text-sm font-medium">Remove all APIs</p>
                                <p className="text-xs text-muted-foreground leading-snug">
                                    Detach every API proxy from this product in one step. Plans stay attached.
                                </p>
                            </div>
                        </button>

                        {/* Delete product */}
                        <button
                            type="button"
                            disabled={isDeleting}
                            onClick={() => setConfirmAction('delete')}
                            className="flex items-start gap-3 rounded-lg border p-3 text-left transition-colors hover:bg-accent/50 disabled:pointer-events-none disabled:opacity-50"
                        >
                            <div className="shrink-0 rounded-lg bg-destructive/10 p-1.5">
                                <Trash2Icon className="size-4 text-destructive" aria-hidden />
                            </div>
                            <div>
                                <p className="text-sm font-medium text-destructive">Delete API product</p>
                                <p className="text-xs text-muted-foreground leading-snug">
                                    Permanently removes the product and its configuration. This action cannot be undone.
                                </p>
                            </div>
                        </button>
                    </div>
                </CardContent>
            </Card>

            {/* Remove all APIs confirmation */}
            <Dialog
                open={confirmAction === 'remove-apis'}
                onOpenChange={open => {
                    if (!open) setConfirmAction(null);
                }}
            >
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>Remove all APIs?</DialogTitle>
                        <DialogDescription>
                            Consumers will lose access through this product until you add APIs again. Plans stay attached.
                        </DialogDescription>
                    </DialogHeader>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setConfirmAction(null)}>
                            Cancel
                        </Button>
                        <Button variant="destructive" disabled={isSaving} onClick={handleRemoveAllApis}>
                            Remove all
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            {/* Delete product confirmation */}
            <Dialog
                open={confirmAction === 'delete'}
                onOpenChange={open => {
                    if (!open) {
                        setConfirmAction(null);
                        setDeleteConfirm('');
                    }
                }}
            >
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>Delete product permanently?</DialogTitle>
                        <DialogDescription>
                            This will permanently delete <strong>{product?.name}</strong> and all its configuration.
                        </DialogDescription>
                    </DialogHeader>
                    <div className="py-2 space-y-2">
                        <Label htmlFor="del-prod-confirm" className="text-sm">
                            Type <span className="font-mono font-semibold">{product?.name}</span> to confirm
                        </Label>
                        <Input
                            id="del-prod-confirm"
                            value={deleteConfirm}
                            onChange={e => setDeleteConfirm(e.target.value)}
                            placeholder={product?.name ?? ''}
                        />
                    </div>
                    <DialogFooter>
                        <Button
                            variant="outline"
                            onClick={() => {
                                setConfirmAction(null);
                                setDeleteConfirm('');
                            }}
                        >
                            Cancel
                        </Button>
                        <Button variant="destructive" disabled={isDeleting || deleteConfirm !== product?.name} onClick={handleDelete}>
                            <Trash2Icon className="size-4" aria-hidden />
                            {isDeleting ? 'Deleting…' : 'Delete permanently'}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    );
}
