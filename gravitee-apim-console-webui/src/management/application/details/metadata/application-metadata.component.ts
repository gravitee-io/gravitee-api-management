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
import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { map, takeUntil, tap } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { ApplicationMetadataService } from '../../../../services-ngx/application-metadata.service';
import { MetadataSaveServices } from '../../../../components/gio-metadata/gio-metadata.component';
import { ApplicationService } from '../../../../services-ngx/application.service';

@Component({
  selector: 'application-metadata',
  templateUrl: './application-metadata.component.html',
  styleUrls: ['./application-metadata.component.scss'],
})
export class ApplicationMetadataComponent implements OnInit, OnDestroy {
  metadataSaveServices: MetadataSaveServices;
  description: string;
  isReadonly = false;

  private unsubscribe$: Subject<void> = new Subject<void>();

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly applicationService: ApplicationService,
    private readonly applicationMetadataService: ApplicationMetadataService,
  ) {}

  ngOnInit() {
    const applicationId = this.activatedRoute.snapshot.params.applicationId;
    this.metadataSaveServices = {
      type: 'Application',
      list: () =>
        this.applicationMetadataService
          .listMetadata(applicationId)
          .pipe(map((metadata) => ({ data: metadata, totalResults: metadata?.length ?? 0 }))),
      create: (newMetadata) => this.applicationMetadataService.createMetadata(applicationId, newMetadata),
      update: (updateMetadata) => this.applicationMetadataService.updateMetadata(applicationId, updateMetadata),
      delete: (metadataKey) => this.applicationMetadataService.deleteMetadata(applicationId, metadataKey),
    };
    this.description = `Create Application metadata to retrieve custom information about your API`;

    this.applicationService
      .getById(applicationId)
      .pipe(
        tap((application) => (this.isReadonly = application.origin === 'KUBERNETES')),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.unsubscribe();
  }
}
