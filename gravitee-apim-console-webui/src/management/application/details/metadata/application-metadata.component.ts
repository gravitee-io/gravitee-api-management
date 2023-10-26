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

import { ApplicationMetadataService } from '../../../../services-ngx/application-metadata.service';
import { UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { MetadataSaveServices } from '../../../../components/gio-metadata/gio-metadata.component';

@Component({
  selector: 'application-metadata',
  template: require('./application-metadata.component.html'),
})
export class ApplicationMetadataComponent implements OnInit {
  metadataSaveServices: MetadataSaveServices;
  description: string;

  constructor(
    private readonly applicationMetadataService: ApplicationMetadataService,
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
  ) {}

  ngOnInit() {
    this.metadataSaveServices = {
      type: 'Application',
      list: () => this.applicationMetadataService.listMetadata(this.ajsStateParams.applicationId),
      create: (newMetadata) => this.applicationMetadataService.createMetadata(this.ajsStateParams.applicationId, newMetadata),
      update: (updateMetadata) => this.applicationMetadataService.updateMetadata(this.ajsStateParams.applicationId, updateMetadata),
      delete: (metadataKey) => this.applicationMetadataService.deleteMetadata(this.ajsStateParams.applicationId, metadataKey),
    };
    this.description = `Create Application metadata to retrieve custom information about your API`;
  }
}
