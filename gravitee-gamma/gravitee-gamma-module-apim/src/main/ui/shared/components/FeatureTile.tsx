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
import type { LucideIcon } from '@gravitee/graphene-core/icons';

export function FeatureTile({ Icon, title, description }: { Icon: LucideIcon; title: string; description: string }) {
    return (
        <div className="space-y-1">
            <div className="flex items-center gap-2">
                <div className="rounded-md bg-primary/10 p-1">
                    <Icon className="size-3 text-primary" aria-hidden />
                </div>
                <p className="text-sm font-semibold">{title}</p>
            </div>
            <p className="text-xs text-muted-foreground leading-relaxed">{description}</p>
        </div>
    );
}
