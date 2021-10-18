/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import * as _ from 'lodash';
const PageComponent: ng.IComponentOptions = {
  controller: class {
    public pageType: string;
    public page: any;

    private fetchersWithSvg: string[] = ['github', 'gitlab', 'bitbucket'];

    type(): string {
      if (this.pageType) {
        return this.pageType;
      } else {
        return this.page.type;
      }
    }

    logoAvailable(fetcherName: string): boolean {
      return _.includes(this.fetchersWithSvg, fetcherName);
    }
  },
  template: require('./page.html'),
  bindings: {
    page: '<',
    pageType: '<',
    edit: '<',
    sidenav: '<',
    sidenavoffset: '<',
  },
};

export default PageComponent;
