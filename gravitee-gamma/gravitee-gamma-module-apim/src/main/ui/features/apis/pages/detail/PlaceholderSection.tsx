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
import { Card } from '@gravitee/graphene-core';

export function PlaceholderSection({ title, description }: { title: string; description?: string }) {
    return (
        <div className="space-y-4">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
                {description ? <p className="text-sm text-muted-foreground">{description}</p> : null}
            </div>
            <Card className="rounded-xl border-dashed p-8 text-center">
                <p className="text-sm text-muted-foreground">{title} coming soon</p>
            </Card>
        </div>
    );
}
