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
import { Component, HostListener, OnInit } from '@angular/core';
import { Api, ApiMetrics, ApiService, Page, PortalService } from '../../../../projects/portal-webclient-sdk/src/lib';
import { Router } from '@angular/router';
import { ApiStatesPipe } from '../../pipes/api-states.pipe';
import { ApiLabelsPipe } from '../../pipes/api-labels.pipe';
import '@gravitee/ui-components/wc/gv-card-list';
import { ConfigurationService } from '../../services/configuration.service';

@Component({
  selector: 'app-homepage',
  templateUrl: './homepage.component.html',
  styleUrls: ['./homepage.component.css'],
})
export class HomepageComponent implements OnInit {
  public homepage: Page;
  public topApis: { item: Api; metric: Promise<ApiMetrics> }[] = [];

  constructor(
    private portalService: PortalService,
    private apiService: ApiService,
    private router: Router,
    private apiStates: ApiStatesPipe,
    private apiLabels: ApiLabelsPipe,
    private config: ConfigurationService,
  ) {}

  ngOnInit() {
    this.portalService.getPages({ homepage: true }).subscribe((response) => {
      this.homepage = response.data[0];
    });

    const size = this.config.get('homepage.featured.size', 9);
    this.apiService.getApis({ filter: 'FEATURED', size }).subscribe((response) => {
      this.topApis = response.data.map((a) => {
        const metric = this.apiService.getApiMetricsByApiId({ apiId: a.id }).toPromise();
        // @ts-ignore
        a.states = this.apiStates.transform(a);
        a.labels = this.apiLabels.transform(a);
        return { item: a, metric };
      });
    });
  }

  @HostListener(':gv-card-full:click', ['$event.detail'])
  goToApi(api: Promise<Api>) {
    Promise.resolve(api).then((_api) => {
      this.router.navigate(['/catalog/api/' + _api.id]);
    });
  }

  @HostListener(':app-gv-page-markdown:navigate', ['$event.detail.pageId'])
  onInternalLinkClick(pageId: string) {
    this.router.navigate(['/documentation/root'], { queryParams: { page: pageId } });
  }
}
