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
import { startWith, takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

import AlertService from '../../services/alert.service';
import { Scope as AlertScope } from '../../entities/alert';

@Component({
  template: '',
  selector: 'documentation-edit-page',
  host: {
    class: 'bootstrap',
  },
})
export class AlertsComponent extends UpgradeComponent {
  private unsubscribe$ = new Subject<void>();

  @Output()
  reload!: EventEmitter<void>;

  private fistChange = true;
  constructor(
    elementRef: ElementRef,
    injector: Injector,
    private readonly activatedRoute: ActivatedRoute,
    @Inject('ajsAlertService') private readonly ajsAlertService: AlertService,
  ) {
    super('alertsComponentAjs', elementRef, injector);
  }

  ngOnInit() {
    const apiId = this.activatedRoute.snapshot.params.apiId;

    this.ngOnChanges({
      activatedRoute: new SimpleChange(null, this.activatedRoute, true),
    });

    this.reload.pipe(startWith({}), takeUntil(this.unsubscribe$)).subscribe(() => {
      Promise.all([
        this.ajsAlertService.listAlerts(AlertScope.API, true, apiId).then((response) => {
          return response.data;
        }),
      ]).then(([alerts]) => {
        // Hack to Force the binding between Angular and AngularJS
        this.ngOnChanges({
          alerts: new SimpleChange(null, alerts, this.fistChange),
        });
        this.fistChange = false;
      });
    });

    super.ngOnInit();
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
    super.ngOnDestroy();
  }
}
