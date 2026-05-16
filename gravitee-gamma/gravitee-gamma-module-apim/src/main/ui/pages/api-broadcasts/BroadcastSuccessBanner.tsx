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
import { Button } from '@gravitee/graphene-core';
import { CircleCheckIcon, RadioIcon } from '@gravitee/graphene-core/icons';

interface BroadcastSuccessBannerProps {
    reach: number;
    onComposeAnother: () => void;
}

export function BroadcastSuccessBanner({ reach, onComposeAnother }: Readonly<BroadcastSuccessBannerProps>) {
    return (
        <div
            className="flex flex-col items-center gap-6 rounded-xl p-8 text-center"
            style={{
                backgroundColor: 'color-mix(in oklab, var(--color-success) 8%, transparent)',
                border: '1px solid color-mix(in oklab, var(--color-success) 25%, transparent)',
            }}
        >
            <div
                className="flex size-14 items-center justify-center rounded-full"
                style={{ backgroundColor: 'color-mix(in oklab, var(--color-success) 15%, transparent)' }}
            >
                <CircleCheckIcon className="size-7 text-success" aria-hidden />
            </div>

            <div className="space-y-1">
                <h2 className="text-lg font-semibold">Broadcast sent</h2>
                <p className="text-sm text-muted-foreground">
                    {reach > 0
                        ? `Your message was delivered to ${reach} recipient${reach !== 1 ? 's' : ''}.`
                        : 'Your message has been sent.'}
                </p>
            </div>

            <Button type="button" variant="outline" size="sm" onClick={onComposeAnother}>
                <RadioIcon className="size-4" aria-hidden />
                Compose another broadcast
            </Button>
        </div>
    );
}
