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
import { Component, OnInit, HostListener } from '@angular/core';
import '@gravitee/ui-components/wc/gv-list';
import '@gravitee/ui-components/wc/gv-info';
import { ApiService, Api, Link, Page } from '@gravitee/ng-portal-webclient';
import { ActivatedRoute, PRIMARY_OUTLET, Router } from '@angular/router';
import { ApiMetrics } from '@gravitee/ng-portal-webclient/model/apiMetrics';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { TranslateService } from '@ngx-translate/core';
import { INavRoute } from 'src/app/services/nav-route.service';

@Component({
  selector: 'app-api-general',
  templateUrl: './api-general.component.html',
  styleUrls: ['./api-general.component.css']
})
export class ApiGeneralComponent implements OnInit {

  currentApi: Promise<Api>;
  currentApiMetrics: Promise<ApiMetrics>;
  homepage: Page;
  linkedApp: Promise<any[]>;
  resources: any[];
  miscellaneous: any[];
  description: string;
  constructor(
    private apiServices: ApiService,
    private route: ActivatedRoute,
    private translateService: TranslateService,
    private router: Router,
  ) { }

  ngOnInit() {
    const apiId = this.route.snapshot.params.apiId;
    if (apiId) {
      this.apiServices.getPagesByApiId({ apiId, homepage: true }).subscribe(response => this.homepage = response.data[0] );
      this.currentApi = this.apiServices.getApiByApiId({ apiId }).toPromise();
      this.currentApiMetrics = this.apiServices.getApiMetricsByApiId({ apiId }).toPromise();

      this.currentApi.then((api) => {
        this.apiServices.getApiLinks({ apiId: api.id }).subscribe(apiLinks => {
            if (apiLinks.slots && apiLinks.slots.aside) {
              apiLinks.slots.aside.forEach((catLinks) => {
                if (catLinks.root) {
                  this.resources = this._buildLinks(apiId, catLinks.links);
                }
              });
            }
          });

        this.description = api.description;

        this.linkedApp = this.apiServices.getSubscriberApplicationsByApiId({ apiId })
          .toPromise()
          .then((response) => {
            return response.data.map((app) => ({
              name: app.name,
              description: app.description,
              picture: (app._links ? app._links.picture : '')
            }));
          })
          .catch(() => []);

        this.translateService.get( [i18n('api.miscellaneous.version'), i18n('api.miscellaneous.lastUpdate'), i18n('api.miscellaneous.publisher')] )
          .subscribe(
            ({
              'api.miscellaneous.version': version,
              'api.miscellaneous.lastUpdate': lastUpdate,
              'api.miscellaneous.publisher': publisher
            }) => {
            this.miscellaneous = [
              { key: version, value: api.version },
              { key: lastUpdate, value: new Date(api.updated_at).toLocaleString(this.translateService.currentLang) },
              { key: publisher, value: api.owner.display_name }
            ];
          });
      });
    }
  }

  _buildLinks(apiId: string, links: Link[]) {
    return links.map(element => {
      let path: string;
      let target: string;
      switch (element.resourceType) {
        case Link.ResourceTypeEnum.External:
          path = element.resourceRef;
          if (path.toLowerCase().startsWith('http')) {
            target = '_blank';
          }
          break;
        case Link.ResourceTypeEnum.Page:
          path = '/catalog/api/' + apiId + '/doc';
          if (element.folder && element.resourceRef !== 'root' ) {
            path += '?folder=' + element.resourceRef;
          } else if (!element.folder) {
            path += '?page=' + element.resourceRef;
          }
          target = '_self';
          break;
        case Link.ResourceTypeEnum.View:
          path = '/catalog/categories/' + element.resourceRef;
          target = '_self';
          break;
      }
      return { title: element.name, path, target };
    });
  }

  @HostListener(':gv-info:click-view', ['$event.detail.tagValue'])
  onClickView(tagValue: string) {
    this.router.navigate(['catalog/categories', tagValue]);
  }

  @HostListener(':gv-info:click-label', ['$event.detail.tagValue'])
  onClickLabel(tagValue: string) {
    this.router.navigate(['catalog/search'], { queryParams: { q: tagValue } });
  }

  @HostListener(':gv-info:click-resource', ['$event.detail'])
  onNavChange(route: INavRoute) {
    if (route.target && route.target === '_blank') {
      window.open(route.path, route.target);
    } else {
      const urlTree = this.router.parseUrl(route.path);
      const path = urlTree.root.children[PRIMARY_OUTLET].segments.join('/');
      this.router.navigate([path], { queryParams: urlTree.queryParams });
    }
  }
}
