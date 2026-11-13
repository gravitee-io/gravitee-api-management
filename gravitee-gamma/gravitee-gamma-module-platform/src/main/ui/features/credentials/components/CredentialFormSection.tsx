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
import type { ReactNode } from 'react';

export function CredentialFormSection({
    index,
    title,
    description,
    children,
}: Readonly<{ index: number; title: string; description: string; children: ReactNode }>) {
    return (
        <section className="flex gap-4">
            <span className="flex size-6 shrink-0 items-center justify-center rounded-full bg-muted text-xs font-medium text-muted-foreground">
                {index}
            </span>
            <div className="flex-1 space-y-4">
                <div className="space-y-1">
                    <h2 className="text-sm font-semibold">{title}</h2>
                    <p className="text-xs text-muted-foreground">{description}</p>
                </div>
                {children}
            </div>
        </section>
    );
}
