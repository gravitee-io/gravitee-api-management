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
import { UpgradeComponent } from '@angular/upgrade/static';
import { Component, ElementRef, Injector, SimpleChange } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { combineLatest, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { GroupService } from '../../../../services-ngx/group.service';
import { ApplicationService } from '../../../../services-ngx/application.service';

@Component({
  template: '',
  selector: 'application-general',
  host: {
    class: 'bootstrap',
  },
})
export class ApplicationGeneralComponent extends UpgradeComponent {
  private unsubscribe$ = new Subject<void>();

  constructor(
    elementRef: ElementRef,
    injector: Injector,
    private readonly activatedRoute: ActivatedRoute,
    private readonly groupService: GroupService,
    private readonly applicationService: ApplicationService,
  ) {
    super('applicationGeneral', elementRef, injector);
  }

  override ngOnInit() {
    const applicationId = this.activatedRoute.snapshot.params.applicationId;
    combineLatest([
      this.groupService.list(),
      this.applicationService.getLastApplicationFetch(applicationId),
      this.applicationService.getApplicationType(applicationId),
    ])
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe({
        next: ([groups, application, applicationType]) => {
          this.ngOnChanges({
            application: new SimpleChange(null, application, true),
            applicationType: new SimpleChange(null, applicationType, true),
            groups: new SimpleChange(
              null,
              groups.filter((group) => group.manageable),
              true,
            ),
            activatedRoute: new SimpleChange(null, this.activatedRoute, true),
          });

          super.ngOnInit();
        },
      });
  }

  override ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();

    super.ngOnDestroy();
  }
}
