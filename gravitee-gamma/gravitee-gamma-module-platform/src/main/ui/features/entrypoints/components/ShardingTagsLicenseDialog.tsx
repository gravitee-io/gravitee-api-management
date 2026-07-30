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
import { Button, Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@gravitee/graphene-core';
import { CheckIcon, LockIcon } from '@gravitee/graphene-core/icons';

import { SHARDING_TAGS_UPGRADE } from '../license/shardingTagsLicense';

const SELF_HOSTED_TRIAL_URL = 'https://gravitee.io/self-hosted-trial';

export function ShardingTagsLicenseDialog({
    open,
    onOpenChange,
}: Readonly<{
    open: boolean;
    onOpenChange: (open: boolean) => void;
}>) {
    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="w-full max-w-md sm:max-w-md">
                <DialogHeader>
                    <div className="flex items-center gap-2">
                        <LockIcon className="size-5 text-muted-foreground" aria-hidden />
                        <DialogTitle>{SHARDING_TAGS_UPGRADE.title}</DialogTitle>
                    </div>
                    <DialogDescription>{SHARDING_TAGS_UPGRADE.description}</DialogDescription>
                </DialogHeader>
                <ul className="space-y-2 py-2 text-sm">
                    {SHARDING_TAGS_UPGRADE.features.map(feature => (
                        <li key={feature} className="flex items-start gap-2">
                            <CheckIcon className="mt-0.5 size-4 shrink-0 text-primary" aria-hidden />
                            <span>{feature}</span>
                        </li>
                    ))}
                </ul>
                <DialogFooter className="sm:justify-end">
                    <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                        Close
                    </Button>
                    <Button asChild>
                        <a href={SELF_HOSTED_TRIAL_URL} target="_blank" rel="noopener noreferrer">
                            Start a free trial
                        </a>
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
