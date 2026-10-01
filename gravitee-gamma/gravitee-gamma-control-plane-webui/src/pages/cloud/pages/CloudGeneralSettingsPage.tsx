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
import { Button, Card, CardContent, Input, Label, Textarea, toast } from '@gravitee/graphene-core';
import { type FormEvent, useCallback, useMemo, useState } from 'react';

import { CloudAccountSettingsHeader } from '../components/CloudAccountSettingsHeader';
import { CLOUD_ACCOUNT_MOCK } from '../cloud.config';

const DESCRIPTION_MAX = 4000;

export function CloudGeneralSettingsPage() {
    const [name, setName] = useState<string>(CLOUD_ACCOUNT_MOCK.name);
    const [description, setDescription] = useState<string>(CLOUD_ACCOUNT_MOCK.description);
    const [hrid, setHrid] = useState<string>(CLOUD_ACCOUNT_MOCK.hrid);
    const [savedName, setSavedName] = useState<string>(CLOUD_ACCOUNT_MOCK.name);
    const [savedDescription, setSavedDescription] = useState<string>(CLOUD_ACCOUNT_MOCK.description);
    const [savedHrid, setSavedHrid] = useState<string>(CLOUD_ACCOUNT_MOCK.hrid);

    const accountDirty = name !== savedName || description !== savedDescription;
    const hridDirty = hrid !== savedHrid;

    const accountValid = name.trim().length >= 2 && description.length <= DESCRIPTION_MAX;
    const hridValid = hrid.trim().length >= 2;

    const canSaveAccount = accountDirty && accountValid;
    const canChangeHrid = CLOUD_ACCOUNT_MOCK.canChangeHrid && hridDirty && hridValid;

    const descriptionHint = useMemo(() => `${description.length} / ${DESCRIPTION_MAX}`, [description.length]);

    const handleSaveAccount = useCallback(
        (event: FormEvent) => {
            event.preventDefault();
            if (!canSaveAccount) return;
            setSavedName(name.trim());
            setSavedDescription(description);
            toast.success('Account has been updated');
        },
        [canSaveAccount, description, name],
    );

    const handleChangeHrid = useCallback(
        (event: FormEvent) => {
            event.preventDefault();
            if (!canChangeHrid) return;
            setSavedHrid(hrid.trim());
            toast.success('Account HRID has been updated');
        },
        [canChangeHrid, hrid],
    );

    return (
        <div className="max-w-3xl space-y-6">
            <CloudAccountSettingsHeader title="Account settings" />

            <Card>
                <CardContent className="space-y-4 pt-6">
                    <form onSubmit={handleSaveAccount} className="space-y-4">
                        <div className="space-y-2">
                            <Label htmlFor="account-name">
                                Name<span className="text-destructive">*</span>
                            </Label>
                            <Input
                                id="account-name"
                                data-testid="accountName"
                                value={name}
                                onChange={event => setName(event.target.value)}
                                placeholder="Name"
                                aria-required
                            />
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="account-description">Description</Label>
                            <Textarea
                                id="account-description"
                                data-testid="accountDescription"
                                value={description}
                                onChange={event => setDescription(event.target.value)}
                                placeholder="Description"
                                rows={3}
                                maxLength={DESCRIPTION_MAX}
                            />
                            <p className="text-right text-xs text-muted-foreground">{descriptionHint}</p>
                        </div>

                        <div className="flex justify-end">
                            <Button type="submit" data-testid="save-account" disabled={!canSaveAccount}>
                                Save account
                            </Button>
                        </div>
                    </form>
                </CardContent>
            </Card>

            <Card>
                <CardContent className="space-y-4 pt-6">
                    <form onSubmit={handleChangeHrid} className="space-y-4">
                        <div className="space-y-2">
                            <Label htmlFor="account-hrid">
                                Human Readable ID<span className="text-destructive">*</span>
                            </Label>
                            <Input
                                id="account-hrid"
                                data-testid="accountHrid"
                                value={hrid}
                                onChange={event => setHrid(event.target.value)}
                                placeholder="Human Readable ID"
                                disabled={!CLOUD_ACCOUNT_MOCK.canChangeHrid}
                                aria-required
                            />
                        </div>

                        <div className="flex justify-end">
                            <Button type="submit" variant="destructive" disabled={!canChangeHrid}>
                                Change HRID
                            </Button>
                        </div>
                    </form>
                </CardContent>
            </Card>
        </div>
    );
}
