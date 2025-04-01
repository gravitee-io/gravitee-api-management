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
import { ActivatedRoute } from '@angular/router';
import { map } from 'rxjs/operators';

import { MetadataSaveServices } from '../../../components/gio-metadata/gio-metadata.component';
import { EnvironmentMetadataService } from '../../../services-ngx/environment-metadata.service';

@Component({
  selector: 'environment-metadata',
  templateUrl: './environment-metadata.component.html',
  styleUrls: ['./environment-metadata.component.scss'],
  standalone: false,
})
export class EnvironmentMetadataComponent implements OnInit {
  metadataSaveServices: MetadataSaveServices;
  description: string;

  constructor(
    private readonly environmentMetadataService: EnvironmentMetadataService,
    private readonly activatedRoute: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.metadataSaveServices = {
      type: 'Global',
      list: () =>
        this.environmentMetadataService.listMetadata().pipe(map((metadata) => ({ data: metadata, totalResults: metadata?.length ?? 0 }))),
      create: (newMetadata) => this.environmentMetadataService.createMetadata(newMetadata),
      update: (updateMetadata) => this.environmentMetadataService.updateMetadata(updateMetadata),
      delete: (metadataKey) => this.environmentMetadataService.deleteMetadata(metadataKey),
    };
    this.description = `Create Global metadata to retrieve custom information about your API`;
  }
}
