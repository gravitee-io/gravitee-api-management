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
import { Button, Card, CardContent, CardDescription, CardHeader, CardTitle, Input, Label, Textarea } from '@gravitee/graphene-core';
import { BoxesIcon, CheckIcon, ClockIcon, ServerIcon, Trash2Icon, UserIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { ConfirmDialog } from '../../../../../shared/components';
import { notify } from '../../../../../shared/notify';
import { SyncStatusBadge } from '../../../components/SyncStatusBadge';
import { useApiProductDetailContext } from '../../../context/ApiProductDetailContext';
import { useDeleteApiProduct } from '../../../hooks/useDeleteApiProduct';
import { useUpdateApiProduct } from '../../../hooks/useUpdateApiProduct';
import { useVerifyApiProductName } from '../../../hooks/useVerifyApiProductName';

function formatDate(iso?: string) {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('en-GB', { dateStyle: 'medium', timeStyle: 'short' });
}

export function ApiProductGeneralPage() {
    const { productId } = useParams<{ productId: string }>();
    const navigate = useNavigate();
    const { product, isLoading } = useApiProductDetailContext();

    const [name, setName] = useState(product?.name ?? '');
    const [version, setVersion] = useState(product?.version ?? '');
    const [description, setDescription] = useState(product?.description ?? '');
    const [confirmAction, setConfirmAction] = useState<'remove-apis' | 'delete' | null>(null);
    const [debouncedName, setDebouncedName] = useState('');

    const [prevProductId, setPrevProductId] = useState(product?.id);
    if (product?.id !== prevProductId) {
        setPrevProductId(product?.id);
        if (product) {
            setName(product.name);
            setVersion(product.version);
            setDescription(product.description ?? '');
            setDebouncedName('');
        }
    }

    useEffect(() => {
        if (!product || name === product.name) return;
        const timer = setTimeout(() => setDebouncedName(name.trim()), 400);
        return () => clearTimeout(timer);
    }, [name, product]);

    // When the name reverts to the saved value, treat debouncedName as empty so the
    // uniqueness hint clears immediately without a synchronous setState in the effect.
    const effectiveDebouncedName = name !== product?.name ? debouncedName : '';

    const { mutate: updateProduct, isPending: isSaving } = useUpdateApiProduct(productId ?? '');
    const { mutate: deleteProduct, isPending: isDeleting } = useDeleteApiProduct();
    const { data: verifyResult, isChecking } = useVerifyApiProductName(effectiveDebouncedName, productId);
    const nameError =
        name !== product?.name && effectiveDebouncedName && verifyResult && !verifyResult.ok
            ? (verifyResult.reason ?? 'Name is already taken.')
            : null;

    const isDirty = product && (name !== product.name || version !== product.version || description !== (product.description ?? ''));
    const canSave = isDirty && name.trim() && version.trim() && !isSaving && !isChecking && !nameError;

    function handleSave(e: React.FormEvent) {
        e.preventDefault();
        if (!canSave || !product) return;
        updateProduct(
            {
                name: name.trim(),
                version: version.trim(),
                description: description.trim() || undefined,
                apiIds: product.apiIds ?? [],
            },
            {
                onSuccess: () => notify.success('API product saved'),
                onError: error => notify.error(error, 'Failed to save API product.'),
            },
        );
    }

    function handleRemoveAllApis() {
        if (!product) return;
        updateProduct(
            { name: product.name, version: product.version, description: product.description, apiIds: [] },
            {
                onSuccess: () => notify.success('All APIs removed from the product'),
                onError: error => notify.error(error, 'Failed to remove APIs from the product.'),
            },
        );
        setConfirmAction(null);
    }

    function handleDelete() {
        if (!productId) return;
        deleteProduct(productId, {
            onSuccess: () => {
                notify.success('API product deleted');
                navigate('../..');
            },
            onError: error => notify.error(error, 'Failed to delete API product.'),
        });
        setConfirmAction(null);
    }

    if (isLoading)
        return (
            <div>
                <p className="text-sm text-muted-foreground">Loading…</p>
            </div>
        );

    return (
        <div className="space-y-5">
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
                                            aria-invalid={Boolean(nameError)}
                                        />
                                        {nameError ? (
                                            <p className="text-xs text-destructive">{nameError}</p>
                                        ) : isChecking && effectiveDebouncedName ? (
                                            <p className="text-xs text-muted-foreground">Checking availability…</p>
                                        ) : verifyResult?.ok && effectiveDebouncedName ? (
                                            <p className="text-xs text-success">Name is available.</p>
                                        ) : null}
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
                                            maxLength={64}
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
                            </div>

                            {/* Right: details panel separated by left border */}
                            <div className="shrink-0 border-l pl-8 space-y-3" style={{ width: '17.5rem' }}>
                                <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">Details</p>
                                <dl className="space-y-2.5">
                                    <div className="flex items-center justify-between gap-2">
                                        <dt className="text-muted-foreground flex items-center gap-1.5 shrink-0 text-xs">
                                            <UserIcon className="size-3" aria-hidden /> Owner
                                        </dt>
                                        <dd
                                            className="text-right text-xs font-medium truncate max-w-40"
                                            title={product?.primaryOwner?.displayName}
                                        >
                                            {product?.primaryOwner?.displayName ?? '—'}
                                        </dd>
                                    </div>
                                    <div className="flex items-center justify-between gap-2">
                                        <dt className="text-muted-foreground flex items-center gap-1.5 shrink-0 text-xs">
                                            <ClockIcon className="size-3" aria-hidden /> Created
                                        </dt>
                                        <dd className="text-right text-xs">{formatDate(product?.createdAt)}</dd>
                                    </div>
                                    <div className="flex items-center justify-between gap-2">
                                        <dt className="text-muted-foreground flex items-center gap-1.5 shrink-0 text-xs">
                                            <ClockIcon className="size-3" aria-hidden /> Updated
                                        </dt>
                                        <dd className="text-right text-xs">{formatDate(product?.updatedAt)}</dd>
                                    </div>
                                    <div className="flex items-center justify-between gap-2">
                                        <dt className="text-muted-foreground flex items-center gap-1.5 shrink-0 text-xs">
                                            <ServerIcon className="size-3" aria-hidden /> Gateway sync
                                        </dt>
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
            <ConfirmDialog
                open={confirmAction === 'remove-apis'}
                onOpenChange={open => !open && setConfirmAction(null)}
                title="Remove all APIs?"
                description="Consumers will lose access through this product until you add APIs again. Plans stay attached."
                confirmLabel="Remove all"
                destructive
                isPending={isSaving}
                onConfirm={handleRemoveAllApis}
            />

            {/* Delete product confirmation */}
            <ConfirmDialog
                open={confirmAction === 'delete'}
                onOpenChange={open => !open && setConfirmAction(null)}
                title="Delete product permanently?"
                description={
                    <>
                        This will permanently delete <strong>{product?.name}</strong> and all its configuration.
                    </>
                }
                confirmLabel="Delete permanently"
                pendingLabel="Deleting…"
                destructive
                confirmKeyword={product?.name}
                icon={<Trash2Icon className="size-4" aria-hidden />}
                isPending={isDeleting}
                onConfirm={handleDelete}
            />
        </div>
    );
}
