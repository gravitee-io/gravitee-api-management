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
    Dialog,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
    Label,
} from '@gravitee/graphene-core';
import { InfoIcon } from '@gravitee/graphene-core/icons';
import { type FormEvent, useCallback, useEffect, useMemo, useState } from 'react';

import {
    CLOUD_ENVIRONMENT_HRID_MAX_LENGTH,
    CLOUD_ENVIRONMENT_HRID_MIN_LENGTH,
    CLOUD_ENVIRONMENT_NAME_MAX_LENGTH,
    CLOUD_ENVIRONMENT_NAME_MIN_LENGTH,
    CLOUD_PRODUCT_OPTIONS,
    type CloudProduct,
} from '../cloud.config';
import { FormSelect } from './FormSelect';

export interface NewEnvironmentPayload {
    readonly name: string;
    readonly hrid: string;
    readonly product: CloudProduct;
}

interface NewEnvironmentDialogProps {
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly onCreate: (payload: NewEnvironmentPayload) => void;
}

const HRID_PATTERN = /^[a-z0-9]+$/;

function isValidHrid(value: string): boolean {
    return (
        value.length >= CLOUD_ENVIRONMENT_HRID_MIN_LENGTH &&
        value.length <= CLOUD_ENVIRONMENT_HRID_MAX_LENGTH &&
        HRID_PATTERN.test(value)
    );
}

function isValidName(value: string): boolean {
    const trimmed = value.trim();
    return trimmed.length >= CLOUD_ENVIRONMENT_NAME_MIN_LENGTH && trimmed.length <= CLOUD_ENVIRONMENT_NAME_MAX_LENGTH;
}

export function NewEnvironmentDialog({ open, onOpenChange, onCreate }: NewEnvironmentDialogProps) {
    const [name, setName] = useState('');
    const [hrid, setHrid] = useState('');
    const [product, setProduct] = useState<CloudProduct>('APIM');
    const [hridDirty, setHridDirty] = useState(false);
    const [creating, setCreating] = useState(false);

    useEffect(() => {
        if (!open) {
            setName('');
            setHrid('');
            setProduct('APIM');
            setHridDirty(false);
            setCreating(false);
        }
    }, [open]);

    const canCreate = useMemo(() => isValidName(name) && isValidHrid(hrid) && product.length > 0, [hrid, name, product]);

    const handleNameChange = useCallback(
        (value: string) => {
            setName(value);
            if (!hridDirty) {
                setHrid(value.toLowerCase().replace(/\s+/g, '').slice(0, CLOUD_ENVIRONMENT_HRID_MAX_LENGTH));
            }
        },
        [hridDirty],
    );

    const handleSubmit = useCallback(
        (event: FormEvent) => {
            event.preventDefault();
            if (!canCreate || creating) return;

            setCreating(true);
            onCreate({ name: name.trim(), hrid, product });
            onOpenChange(false);
        },
        [canCreate, creating, hrid, name, onCreate, onOpenChange, product],
    );

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="w-full max-w-sm">
                <form onSubmit={handleSubmit}>
                    <DialogHeader>
                        <DialogTitle>Create New Environment</DialogTitle>
                    </DialogHeader>

                    <div className="space-y-3 py-2">
                        <div className="space-y-2">
                            <Label htmlFor="new-environment-name">
                                Name<span className="text-destructive">*</span>
                            </Label>
                            <Input
                                id="new-environment-name"
                                data-testid="new-environment-name"
                                value={name}
                                onChange={event => handleNameChange(event.target.value)}
                                placeholder="Name"
                                maxLength={CLOUD_ENVIRONMENT_NAME_MAX_LENGTH}
                                autoFocus
                                aria-required
                            />
                            <p className="text-xs text-muted-foreground">Keep it short and meaningful.</p>
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="new-environment-product">
                                Product<span className="text-destructive">*</span>
                            </Label>
                            <FormSelect
                                id="new-environment-product"
                                data-testid="new-environment-product"
                                value={product}
                                onChange={event => setProduct(event.target.value as CloudProduct)}
                                aria-required
                            >
                                {CLOUD_PRODUCT_OPTIONS.map(option => (
                                    <option key={option} value={option}>
                                        {option}
                                    </option>
                                ))}
                            </FormSelect>
                        </div>

                        <div className="space-y-2">
                            <div className="flex items-start gap-2">
                                <div className="min-w-0 flex-1 space-y-2">
                                    <Label htmlFor="new-environment-hrid">
                                        Human Readable ID<span className="text-destructive">*</span>
                                    </Label>
                                    <Input
                                        id="new-environment-hrid"
                                        data-testid="new-environment-hrid"
                                        value={hrid}
                                        onChange={event => {
                                            setHridDirty(true);
                                            setHrid(event.target.value);
                                        }}
                                        placeholder="Human Readable ID"
                                        maxLength={CLOUD_ENVIRONMENT_HRID_MAX_LENGTH}
                                        aria-required
                                    />
                                    <p className="text-xs text-muted-foreground">
                                        Max 16 characters. Use lowercase letters only. No spaces or special characters allowed.
                                    </p>
                                </div>
                                <InfoIcon
                                    className="mt-7 size-4 shrink-0 text-muted-foreground"
                                    aria-label="The HRID is used to build public API URLs for services hosted by Gravitee. Please choose it carefully, as it will be part of your end-user facing URLs."
                                />
                            </div>
                        </div>
                    </div>

                    <DialogFooter>
                        <Button type="submit" data-testid="create-new-environment-button" disabled={!canCreate || creating}>
                            Create
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}
