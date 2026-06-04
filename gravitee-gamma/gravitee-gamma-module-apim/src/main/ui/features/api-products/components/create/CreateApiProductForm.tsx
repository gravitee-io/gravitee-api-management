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
import { Button, Card, CardContent, Input, Label, Textarea } from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useState } from 'react';

import { notify } from '../../../../shared/notify';
import { useCreateApiProduct } from '../../hooks/useCreateApiProduct';
import { useVerifyApiProductName } from '../../hooks/useVerifyApiProductName';
import type { CreateApiProductRequest } from '../../types/apiProduct';

interface CreateApiProductFormProps {
    onBack: () => void;
    onCreated: (productId: string) => void;
}

export function CreateApiProductForm({ onBack, onCreated }: CreateApiProductFormProps) {
    const [name, setName] = useState('');
    const [version, setVersion] = useState('1.0.0');
    const [description, setDescription] = useState('');
    const [debouncedName, setDebouncedName] = useState('');
    const [touched, setTouched] = useState({ name: false, version: false });

    const { mutate: createProduct, isPending, error } = useCreateApiProduct();
    const { data: verifyResult, isChecking } = useVerifyApiProductName(debouncedName);

    useEffect(() => {
        const timer = setTimeout(() => setDebouncedName(name.trim()), 400);
        return () => clearTimeout(timer);
    }, [name]);

    const nameError = (() => {
        if (touched.name && !name.trim()) return 'Name is required.';
        if (debouncedName && verifyResult && !verifyResult.ok) return verifyResult.reason ?? 'Name is already taken.';
        return null;
    })();

    const versionError = touched.version && !version.trim() ? 'Version is required.' : null;

    const canSubmit = name.trim() && version.trim() && !isChecking && (!verifyResult || verifyResult.ok) && !isPending;

    function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        setTouched({ name: true, version: true });
        if (!name.trim() || !version.trim()) return;
        if (verifyResult && !verifyResult.ok) return;

        const request: CreateApiProductRequest = {
            name: name.trim(),
            version: version.trim(),
            description: description.trim() || undefined,
        };
        createProduct(request, {
            onSuccess: product => {
                notify.success('API product created');
                onCreated(product.id);
            },
        });
    }

    return (
        <div className="space-y-6 max-w-3xl">
            <div className="space-y-1">
                <Button type="button" variant="ghost" size="sm" onClick={onBack} className="-ml-2 mb-1 text-muted-foreground">
                    <ArrowLeftIcon className="size-4" aria-hidden />
                    Go back to API Products
                </Button>
                <h1 className="text-2xl font-semibold tracking-tight">Create API Product</h1>
                <p className="text-sm text-muted-foreground">
                    Fill in the details below. You can attach APIs and plans from the product after it is created.
                </p>
            </div>

            <form onSubmit={handleSubmit}>
                <Card>
                    <CardContent className="pt-6 space-y-6">
                        <p className="text-base font-semibold">Product Details</p>

                        <div className="flex gap-6">
                            {/* Name */}
                            <div className="flex-1 space-y-2">
                                <Label htmlFor="product-name">
                                    Name <span className="text-destructive">*</span>
                                </Label>
                                <Input
                                    id="product-name"
                                    value={name}
                                    onChange={e => setName(e.target.value)}
                                    onBlur={() => setTouched(prev => ({ ...prev, name: true }))}
                                    placeholder="My API Product"
                                    aria-describedby={nameError ? 'name-error' : undefined}
                                    aria-invalid={Boolean(nameError)}
                                />
                                {nameError ? (
                                    <p id="name-error" className="text-xs text-destructive">
                                        {nameError}
                                    </p>
                                ) : isChecking && debouncedName ? (
                                    <p className="text-xs text-muted-foreground">Checking availability…</p>
                                ) : verifyResult?.ok && debouncedName ? (
                                    <p className="text-xs text-success">Name is available.</p>
                                ) : null}
                            </div>

                            {/* Version */}
                            <div className="w-36 space-y-2">
                                <Label htmlFor="product-version">
                                    Version <span className="text-destructive">*</span>
                                </Label>
                                <Input
                                    id="product-version"
                                    value={version}
                                    onChange={e => setVersion(e.target.value)}
                                    onBlur={() => setTouched(prev => ({ ...prev, version: true }))}
                                    placeholder="1.0.0"
                                    aria-describedby={versionError ? 'version-error' : undefined}
                                    aria-invalid={Boolean(versionError)}
                                />
                                {versionError ? (
                                    <p id="version-error" className="text-xs text-destructive">
                                        {versionError}
                                    </p>
                                ) : null}
                            </div>
                        </div>

                        {/* Description */}
                        <div className="space-y-2">
                            <Label htmlFor="product-description">Description</Label>
                            <Textarea
                                id="product-description"
                                value={description}
                                onChange={e => setDescription(e.target.value)}
                                placeholder="Describe what this product offers to consumers…"
                                rows={4}
                                style={{ fieldSizing: 'fixed' } as React.CSSProperties}
                            />
                        </div>

                        {error ? (
                            <p className="text-sm text-destructive rounded-md border border-destructive/20 bg-destructive/5 px-3 py-2">
                                {error.message}
                            </p>
                        ) : null}

                        <div className="flex items-center justify-between pt-2">
                            <Button type="button" variant="outline" onClick={onBack}>
                                Cancel
                            </Button>
                            <Button type="submit" disabled={!canSubmit}>
                                {isPending ? 'Creating…' : 'Create API Product'}
                            </Button>
                        </div>
                    </CardContent>
                </Card>
            </form>
        </div>
    );
}
