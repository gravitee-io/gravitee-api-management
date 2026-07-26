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
import { Button, Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@gravitee/graphene-core';
import { CircleCheckIcon } from '@gravitee/graphene-core/icons';

import { REQUEST_ENTERPRISE_LICENSE_URL, type FeatureUpgradeContent } from '../../features/license/apimFeatures';

/**
 * Upgrade dialog shown when a user interacts with an APIM feature that is not
 * covered by the current organization license. Mirrors the Gamma home
 * `UpgradeDialog` pattern (icon header, feature checklist, enterprise CTA).
 */
export function FeatureUpgradeDialog({
    content,
    open,
    onOpenChange,
}: {
    readonly content: FeatureUpgradeContent;
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
}) {
    const { Icon, title, description, features } = content;

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="w-96 gap-0 overflow-hidden p-0">
                <div className="flex flex-col items-center gap-3 bg-primary/10 px-6 pt-10 pb-8 text-center">
                    <div className="flex size-12 items-center justify-center rounded-xl bg-background ring-1 ring-foreground/10">
                        <Icon className="size-8" aria-hidden />
                    </div>
                    <DialogHeader className="gap-1.5">
                        <DialogTitle className="text-center text-xl font-semibold">{title}</DialogTitle>
                        <DialogDescription className="text-center">{description}</DialogDescription>
                    </DialogHeader>
                </div>

                <ul className="flex flex-col gap-2.5 px-6 py-5">
                    {features.map(feature => (
                        <li key={feature} className="flex items-start gap-2.5 text-sm">
                            <CircleCheckIcon className="mt-0.5 size-4 shrink-0 text-success" aria-hidden />
                            <span>{feature}</span>
                        </li>
                    ))}
                </ul>

                <div className="px-6 pb-6">
                    <Button asChild variant="outline" className="w-full rounded-full">
                        <a href={REQUEST_ENTERPRISE_LICENSE_URL} target="_blank" rel="noreferrer">
                            Request an enterprise license
                        </a>
                    </Button>
                </div>
            </DialogContent>
        </Dialog>
    );
}
