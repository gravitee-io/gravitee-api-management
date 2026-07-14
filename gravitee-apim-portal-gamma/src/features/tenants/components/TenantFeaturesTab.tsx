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
import { Button, Label, Switch } from '@gravitee/graphene-core';
import { useEffect, useState } from 'react';

import { notify } from '../../../shared/notify/notify';
import {
    PORTAL_TENANT_FEATURE_LABELS,
    type PortalTenant,
    type PortalTenantFeatures,
} from '../types/portal-tenant.types';

interface TenantFeaturesTabProps {
    readonly tenant: PortalTenant;
    readonly onSave: (features: PortalTenantFeatures) => Promise<void>;
}

const FEATURE_KEYS = Object.keys(PORTAL_TENANT_FEATURE_LABELS) as Array<keyof PortalTenantFeatures>;

export function TenantFeaturesTab({ tenant, onSave }: TenantFeaturesTabProps) {
    const [features, setFeatures] = useState<PortalTenantFeatures>(tenant.features);
    const [isSaving, setIsSaving] = useState(false);

    useEffect(() => {
        setFeatures(tenant.features);
    }, [tenant.features]);

    const toggleFeature = (key: keyof PortalTenantFeatures) => {
        setFeatures(previous => ({
            ...previous,
            [key]: !previous[key],
        }));
    };

    const handleSave = async () => {
        setIsSaving(true);
        try {
            await onSave(features);
            notify.success('Features updated');
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <div className="space-y-6">
            <div>
                <h2 className="text-lg font-semibold">Features</h2>
                <p className="text-sm text-muted-foreground">
                    Control which portal capabilities are available to users in this tenant.
                </p>
            </div>

            <div className="divide-y rounded-lg border">
                {FEATURE_KEYS.map(key => {
                    const meta = PORTAL_TENANT_FEATURE_LABELS[key];
                    const enabled = features[key];

                    return (
                        <div key={key} className="flex items-start justify-between gap-4 px-4 py-4">
                            <div className="min-w-0">
                                <Label htmlFor={`tenant-feature-${key}`} className="font-medium">
                                    {meta.title}
                                </Label>
                                <p className="text-sm text-muted-foreground">{meta.description}</p>
                            </div>
                            <Switch
                                id={`tenant-feature-${key}`}
                                checked={enabled}
                                onCheckedChange={() => toggleFeature(key)}
                                className="shrink-0"
                            />
                        </div>
                    );
                })}
            </div>

            <div className="flex justify-end">
                <Button onClick={() => void handleSave()} disabled={isSaving}>
                    {isSaving ? 'Saving…' : 'Save changes'}
                </Button>
            </div>
        </div>
    );
}
