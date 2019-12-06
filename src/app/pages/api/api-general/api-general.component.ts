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
import { Component, OnInit } from '@angular/core';
import '@gravitee/ui-components/wc/gv-list';
import '@gravitee/ui-components/wc/gv-info-api';
import { ApiService, Api, Page } from '@gravitee/ng-portal-webclient';
import { ActivatedRoute } from '@angular/router';
import { ApiMetrics } from '@gravitee/ng-portal-webclient/model/apiMetrics';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { TranslateService } from '@ngx-translate/core';

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
  resources: string[];
  miscellaneous: any[];
  isOnError: boolean;
  description: string;
  constructor(
    private apiServices: ApiService,
    private route: ActivatedRoute,
    private translateService: TranslateService,
  ) { }

  ngOnInit() {
    const apiId = this.route.snapshot.params.apiId;
    if (apiId) {
      this.apiServices.getPagesByApiId({ apiId, homepage: true }).subscribe(response => this.homepage = response.data[0] );
      this.currentApi = this.apiServices.getApiByApiId({ apiId }).toPromise();
      this.currentApiMetrics = this.apiServices.getApiMetricsByApiId({ apiId }).toPromise();

      this.currentApi.then((api) => {
        // **** TODO : removed mocked data
        this.resources = ['Repository', 'Homepage', 'Licence', 'Changelog', 'Download Extension'];
        // ****

        this.description = api.description;

        this.apiServices.getSubscriberApplicationsByApiId({ apiId }).subscribe((response) => {
          this.linkedApp = Promise.resolve(
            response.data.map((app) => ({ name: app.name, description: app.description, picture: (app._links ? app._links.picture : '') }))
          );
        });

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
}
