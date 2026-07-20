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
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Field,
    FieldLabel,
    Input,
} from '@gravitee/graphene-core';
import { useEffect, useState } from 'react';

interface AddNamedItemDialogProps {
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly onAdd: (input: { name: string; description: string }) => void;
    readonly title: string;
    readonly description: string;
    readonly namePlaceholder: string;
    readonly submitLabel: string;
}

export function AddNamedItemDialog({
    open,
    onOpenChange,
    onAdd,
    title,
    description,
    namePlaceholder,
    submitLabel,
}: AddNamedItemDialogProps) {
    const [name, setName] = useState('');
    const [itemDescription, setItemDescription] = useState('');

    useEffect(() => {
        if (!open) {
            setName('');
            setItemDescription('');
        }
    }, [open]);

    const handleSubmit = () => {
        const trimmed = name.trim();
        if (!trimmed) {
            return;
        }
        onAdd({ name: trimmed, description: itemDescription.trim() });
        onOpenChange(false);
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent style={{ width: 'min(92vw, 28rem)' }}>
                <DialogHeader>
                    <DialogTitle>{title}</DialogTitle>
                    <DialogDescription>{description}</DialogDescription>
                </DialogHeader>

                <form
                    className="space-y-4 py-2"
                    onSubmit={event => {
                        event.preventDefault();
                        handleSubmit();
                    }}
                >
                    <Field>
                        <FieldLabel htmlFor="item-name">Name</FieldLabel>
                        <Input
                            id="item-name"
                            value={name}
                            onChange={event => setName(event.target.value)}
                            placeholder={namePlaceholder}
                            autoFocus
                            required
                        />
                    </Field>
                    <Field>
                        <FieldLabel htmlFor="item-description">Description</FieldLabel>
                        <Input
                            id="item-description"
                            value={itemDescription}
                            onChange={event => setItemDescription(event.target.value)}
                            placeholder="Optional short description"
                        />
                    </Field>
                    <DialogFooter>
                        <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                            Cancel
                        </Button>
                        <Button type="submit" disabled={!name.trim()}>
                            {submitLabel}
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}
