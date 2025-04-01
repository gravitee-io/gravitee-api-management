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

import { Component, ElementRef, EventEmitter, Inject, Injector, Output, SimpleChange } from '@angular/core';
import { UpgradeComponent } from '@angular/upgrade/static';
import { ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { Scope as AlertScope } from '../../../entities/alert';
import AlertService from '../../../services/alert.service';
import NotifierService from '../../../services/notifier.service';
import { ApiService } from '../../../services-ngx/api.service';

@Component({
  template: '',
  selector: 'alert-ajs',
  standalone: false,
  host: {
    class: 'bootstrap',
  },
})
export class AlertComponent extends UpgradeComponent {
  private unsubscribe$ = new Subject<void>();

  @Output()
  reload!: EventEmitter<void>;

  constructor(
    elementRef: ElementRef,
    injector: Injector,
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiService,
    @Inject('ajsAlertService') private readonly ajsAlertService: AlertService,
    @Inject('ajsNotifierService') private readonly ajsNotifierService: NotifierService,
  ) {
    super('alertComponentAjs', elementRef, injector);
  }

  override ngOnInit() {
    const apiId = this.activatedRoute.snapshot.params.apiId;
    const alertId = this.activatedRoute.snapshot.params.alertId;

    const statusPromise = apiId
      ? this.ajsAlertService.getStatus(AlertScope.API, apiId).then((response) => response.data)
      : this.ajsAlertService.getStatus(AlertScope.ENVIRONMENT).then((response) => response.data);

    const alertsPromise = apiId
      ? this.ajsAlertService.listAlerts(AlertScope.API, true, apiId).then((response) => response.data)
      : this.ajsAlertService.listAlerts(AlertScope.ENVIRONMENT, true).then((response) => response.data);

    Promise.all([
      statusPromise,
      this.ajsNotifierService.list().then((response) => response.data),
      alertsPromise,
      Promise.resolve(alertId ? 'detail' : 'create'),
      apiId ? this.apiService.get(apiId).toPromise() : Promise.resolve(null),
    ]).then(([status, notifiers, alerts, mode, resolvedApi]) => {
      // Hack to Force the binding between Angular and AngularJS
      this.ngOnChanges({
        activatedRoute: new SimpleChange(null, this.activatedRoute, true),
        status: new SimpleChange(null, status, true),
        notifiers: new SimpleChange(null, notifiers, true),
        alerts: new SimpleChange(null, alerts, true),
        mode: new SimpleChange(null, mode, true),
        resolvedApi: new SimpleChange(null, resolvedApi, true),
      });

      super.ngOnInit();
    });

    this.reload.pipe(takeUntil(this.unsubscribe$)).subscribe(() => {
      const listAlertPromise = apiId
        ? this.ajsAlertService.listAlerts(AlertScope.API, true, apiId).then((response) => response.data)
        : this.ajsAlertService.listAlerts(AlertScope.ENVIRONMENT, true).then((response) => response.data);
      Promise.all([listAlertPromise]).then(([alerts]) => {
        // Hack to Force the binding between Angular and AngularJS
        this.ngOnChanges({
          alerts: new SimpleChange(null, alerts, false),
        });
      });
    });
  }

  override ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
    super.ngOnDestroy();
  }
}
