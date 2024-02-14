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
import { Component, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { distinctUntilChanged, map, takeUntil } from 'rxjs/operators';
import { isEqual } from 'lodash';
import { MonacoEditorLanguageConfig } from '@gravitee/ui-particles-angular';
import { UntypedFormControl } from '@angular/forms';

import { Event } from '../../../../entities/management-api-v2';
import { ApiEventsV2Service } from '../../../../services-ngx/api-events-v2.service';

@Component({
  selector: 'api-deployment-info',
  templateUrl: './api-history-v4-deployment-info.component.html',
  styleUrls: ['./api-history-v4-deployment-info.component.scss'],
})
export class ApiHistoryV4DeploymentInfoComponent implements OnDestroy {
  public languageConfig: MonacoEditorLanguageConfig = { language: 'json', schemas: [] };
  public control: UntypedFormControl;

  private readonly unsubscribe$: Subject<void> = new Subject<void>();

  DEPLOYMENT_NUMBER_PROPERTY = 'DEPLOYMENT_NUMBER';
  LABEL_PROPERTY = 'DEPLOYMENT_LABEL';
  private apiId = this.activatedRoute.snapshot.params.apiId;
  private apiVersionId = this.activatedRoute.snapshot.params.apiVersionId;

  protected deploymentEventWithFormControl$ = this.eventsService.findById(this.apiId, this.apiVersionId).pipe(
    distinctUntilChanged(isEqual),
    map((deploymentEvent: Event) => ({
      control: new UntypedFormControl({
        value: this.getJsonDefinitionFromEvent(deploymentEvent),
        disabled: true,
      }),
      deploymentEvent,
    })),
    takeUntil(this.unsubscribe$),
  );

  constructor(private readonly eventsService: ApiEventsV2Service, private readonly activatedRoute: ActivatedRoute) {}

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  getJsonDefinitionFromEvent(event: Event) {
    const payload = JSON.parse(event.payload);
    const definition = JSON.parse(payload.definition);
    return JSON.stringify(definition, null, 2);
  }
}
