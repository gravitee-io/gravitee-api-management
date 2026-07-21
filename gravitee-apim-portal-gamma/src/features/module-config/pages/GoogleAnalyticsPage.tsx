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
import { Button, Card, CardContent, Field, FieldLabel, Input, Switch } from '@gravitee/graphene-core';
import { useEffect, useState } from 'react';

import { notify } from '../../../shared/notify/notify';
import { MODULE_CONFIG_SECTION_META } from '../types';

const STORAGE_KEY = 'portal-gamma-google-analytics';

interface GoogleAnalyticsSettings {
    measurementId: string;
    enabled: boolean;
    anonymizeIp: boolean;
    trackOutboundLinks: boolean;
}

const DEFAULT_SETTINGS: GoogleAnalyticsSettings = {
    measurementId: '',
    enabled: false,
    anonymizeIp: true,
    trackOutboundLinks: true,
};

function loadSettings(): GoogleAnalyticsSettings {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        if (!raw) {
            return DEFAULT_SETTINGS;
        }
        return { ...DEFAULT_SETTINGS, ...(JSON.parse(raw) as Partial<GoogleAnalyticsSettings>) };
    } catch {
        return DEFAULT_SETTINGS;
    }
}

export function GoogleAnalyticsPage() {
    const [settings, setSettings] = useState<GoogleAnalyticsSettings>(DEFAULT_SETTINGS);

    useEffect(() => {
        setSettings(loadSettings());
    }, []);

    const meta = MODULE_CONFIG_SECTION_META['google-analytics'];

    const handleSave = () => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
        notify.success('Google Analytics settings saved.');
    };

    return (
        <div className="max-w-3xl space-y-6 p-6">
            <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-bold tracking-tight">{meta.title}</h1>
                    <p className="text-sm text-muted-foreground">{meta.description}</p>
                </div>
                <Button type="button" onClick={handleSave}>
                    Save
                </Button>
            </div>

            <div className="space-y-6">
                <Card>
                    <CardContent className="space-y-5 pt-6">
                        <Field>
                            <FieldLabel htmlFor="ga-measurement-id">Measurement ID</FieldLabel>
                            <Input
                                id="ga-measurement-id"
                                value={settings.measurementId}
                                onChange={event =>
                                    setSettings(current => ({
                                        ...current,
                                        measurementId: event.target.value,
                                    }))
                                }
                                placeholder="G-XXXXXXXXXX"
                            />
                        </Field>

                        <label className="flex w-fit items-center gap-3 text-sm">
                            <span>Enable tracking</span>
                            <Switch
                                checked={settings.enabled}
                                onCheckedChange={checked =>
                                    setSettings(current => ({ ...current, enabled: checked === true }))
                                }
                                aria-label="Enable Google Analytics tracking"
                            />
                        </label>

                        <label className="flex w-fit items-center gap-3 text-sm">
                            <span>Anonymize IP addresses</span>
                            <Switch
                                checked={settings.anonymizeIp}
                                onCheckedChange={checked =>
                                    setSettings(current => ({ ...current, anonymizeIp: checked === true }))
                                }
                                aria-label="Anonymize IP addresses"
                            />
                        </label>

                        <label className="flex w-fit items-center gap-3 text-sm">
                            <span>Track outbound links</span>
                            <Switch
                                checked={settings.trackOutboundLinks}
                                onCheckedChange={checked =>
                                    setSettings(current => ({
                                        ...current,
                                        trackOutboundLinks: checked === true,
                                    }))
                                }
                                aria-label="Track outbound links"
                            />
                        </label>
                    </CardContent>
                </Card>

                <Card>
                    <CardContent className="space-y-2 pt-6">
                        <p className="text-sm font-semibold">How it works</p>
                        <p className="text-sm text-muted-foreground">
                            When enabled, portals inject the Google Analytics tag using your measurement ID.
                            Settings are stored locally in this POC and are not pushed to a backend.
                        </p>
                    </CardContent>
                </Card>
            </div>
        </div>
    );
}
