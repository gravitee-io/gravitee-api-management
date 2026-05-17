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
import { useLocation } from 'react-router-dom';

/**
 * Returns the base path up to and including `/<segment>/<id>`, slicing off
 * any child route segments. Used by detail layout components to build sidebar
 * navigation links that are stable regardless of the active child route.
 */
export function useDetailBasePath(segment: string, id: string | undefined): string {
    const { pathname } = useLocation();
    if (!id) return pathname;
    const marker = `/${segment}/${id}`;
    const idx = pathname.indexOf(marker);
    return idx >= 0 ? pathname.slice(0, idx + marker.length) : pathname;
}
