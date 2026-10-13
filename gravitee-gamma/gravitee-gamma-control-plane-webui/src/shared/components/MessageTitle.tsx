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
import { EmptyTitle } from '@gravitee/graphene-core';

/**
 * The title of a message shown in place of a page. It is the page heading, in place of the one the page
 * would have rendered. Graphene styles a bare h1 as a page title: the classes are EmptyTitle's own, so the
 * message keeps its size.
 */
export function MessageTitle({ children }: { readonly children: string }) {
    return (
        <EmptyTitle>
            <h1 className="text-sm font-medium tracking-tight">{children}</h1>
        </EmptyTitle>
    );
}
