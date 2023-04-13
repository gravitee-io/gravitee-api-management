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
import { Component, Inject, OnInit } from '@angular/core';

import { MetadataSaveServices } from '../../../../../components/gio-metadata/gio-metadata.component';
import { ApiService } from '../../../../../services-ngx/api.service';
import { UIRouterStateParams } from '../../../../../ajs-upgraded-providers';

@Component({
  selector: 'api-portal-documentation-metadata',
  template: require('./api-portal-documentation-metadata.component.html'),
  styles: [require('./api-portal-documentation-metadata.component.scss')],
})
export class ApiPortalDocumentationMetadataComponent implements OnInit {
  metadataSaveServices: MetadataSaveServices;
  description: string;

  constructor(private readonly apiService: ApiService, @Inject(UIRouterStateParams) private readonly ajsStateParams) {}

  ngOnInit() {
    this.metadataSaveServices = {
      type: 'API',
      list: () => this.apiService.listMetadata(this.ajsStateParams.apiId),
      create: (newMetadata) => this.apiService.createMetadata(this.ajsStateParams.apiId, newMetadata),
      update: (updateMetadata) => this.apiService.updateMetadata(this.ajsStateParams.apiId, updateMetadata),
      delete: (metadataKey) => this.apiService.deleteMetadata(this.ajsStateParams.apiId, metadataKey),
    };
    this.description = `Create API metadata to retrieve custom information about your API`;
  }
}
