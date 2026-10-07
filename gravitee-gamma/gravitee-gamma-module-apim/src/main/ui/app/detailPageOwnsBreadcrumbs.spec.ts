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
import { detailPageOwnsBreadcrumbs, moduleShellLayout } from './detailPageOwnsBreadcrumbs';

describe('detailPageOwnsBreadcrumbs', () => {
    it('leaves breadcrumbs to the module shell on list and create pages', () => {
        expect(detailPageOwnsBreadcrumbs('/environments/DEFAULT/apim/apis')).toBe(false);
        expect(detailPageOwnsBreadcrumbs('/environments/DEFAULT/apim/apis/new')).toBe(false);
        expect(detailPageOwnsBreadcrumbs('/environments/DEFAULT/apim/api-products')).toBe(false);
    });

    it('lets an API or API Product detail page own the breadcrumb', () => {
        expect(detailPageOwnsBreadcrumbs('/environments/DEFAULT/apim/apis/abc-123')).toBe(true);
        expect(detailPageOwnsBreadcrumbs('/environments/DEFAULT/apim/apis/abc-123/plans')).toBe(true);
        expect(detailPageOwnsBreadcrumbs('/environments/DEFAULT/apim/api-products/prod-1')).toBe(true);
    });

    it('pushes shell breadcrumbs on a list and leaves them off a detail page', () => {
        const navigation = { nav: true };
        const breadcrumbs = [{ label: 'API Proxies' }];
        const list = '/environments/DEFAULT/apim/apis';
        const detail = '/environments/DEFAULT/apim/apis/abc-123/overview';

        expect(moduleShellLayout(detailPageOwnsBreadcrumbs(list), navigation, breadcrumbs)).toEqual({
            navigation,
            breadcrumbs,
        });
        expect(moduleShellLayout(detailPageOwnsBreadcrumbs(detail), navigation, breadcrumbs)).toEqual({ navigation });
    });
});
