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

/** API and API Product detail pages set their own breadcrumb. Lists and `/new` do not. */
const DETAIL_PAGE = /\/(?:apis|api-products)\/(?!new(?:\/|$))[^/]+/;

export function detailPageOwnsBreadcrumbs(pathname: string): boolean {
    return DETAIL_PAGE.test(pathname);
}

/** Detail pages omit the shell breadcrumb so the page can set the API name. Lists keep it. */
export function moduleShellLayout<N, B>(ownsBreadcrumbs: boolean, navigation: N, breadcrumbs: B): { navigation: N; breadcrumbs?: B } {
    return ownsBreadcrumbs ? { navigation } : { navigation, breadcrumbs };
}
