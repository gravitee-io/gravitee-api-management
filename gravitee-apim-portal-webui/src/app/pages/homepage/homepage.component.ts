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
import { CUSTOM_ELEMENTS_SCHEMA, Component, HostListener, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { NgIf } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';

import { Api, ApiMetrics, ApiService, Page, PortalService } from '../../../../projects/portal-webclient-sdk/src/lib';
import { ApiStatesPipe } from '../../pipes/api-states.pipe';
import { ApiLabelsPipe } from '../../pipes/api-labels.pipe';
import '@gravitee/ui-components/wc/gv-card-list';
import { ConfigurationService } from '../../services/configuration.service';
import { GvPageComponent } from '../../components/gv-page/gv-page.component';

@Component({
  selector: 'app-homepage',
  templateUrl: './homepage.component.html',
  styleUrls: ['./homepage.component.css'],
  imports: [NgIf, GvPageComponent, TranslatePipe],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class HomepageComponent implements OnInit {
  private portalService = inject(PortalService);
  private apiService = inject(ApiService);
  private router = inject(Router);
  private apiStates = inject(ApiStatesPipe);
  private apiLabels = inject(ApiLabelsPipe);
  private config = inject(ConfigurationService);

  public homepage: Page;
  public topApis: { item: Api; metric: Promise<ApiMetrics> }[] = [];
  public pageBaseUrl = '/documentation/root';

  ngOnInit() {
    this.portalService.getPages({ homepage: true }).subscribe(response => {
      this.homepage = response.data[0];
    });

    const size = this.config.get('homepage.featured.size', 9);
    this.apiService.getApis({ filter: 'FEATURED', size }).subscribe(response => {
      this.topApis = response.data.map(a => {
        const metric = this.apiService.getApiMetricsByApiId({ apiId: a.id }).toPromise();
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        a.states = this.apiStates.transform(a);
        a.labels = this.apiLabels.transform(a);
        return { item: a, metric };
      });
    });
  }

  @HostListener(':gv-card-full:click', ['$event.detail'])
  goToApi(api: Promise<Api>) {
    Promise.resolve(api).then(_api => {
      this.router.navigate(['/catalog/api/' + _api.id]);
    });
  }
}
