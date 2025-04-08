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
import { combineLatest, Subject } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { takeUntil } from 'rxjs/operators';

import { GroupService } from '../../../../services-ngx/group.service';
import { ApplicationTypesService } from '../../../../services-ngx/application-types.service';

@Component({
  template: '',
  selector: 'create-application',
  host: {
    class: 'bootstrap',
  },
  standalone: false,
})
export class ApplicationCreationComponent extends UpgradeComponent {
  private unsubscribe$ = new Subject<void>();

  constructor(
    elementRef: ElementRef,
    injector: Injector,
    private readonly activatedRoute: ActivatedRoute,
    private readonly applicationTypeService: ApplicationTypesService,
    private readonly groupService: GroupService,
  ) {
    super('createApplication', elementRef, injector);
  }

  override ngOnInit() {
    combineLatest([this.applicationTypeService.deprecatedGetEnabledApplicationTypes(), this.groupService.list()])
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe({
        next: ([applicationTypes, groupList]) => {
          this.ngOnChanges({
            enabledApplicationTypes: new SimpleChange(null, applicationTypes, true),
            groups: new SimpleChange(null, groupList, true),
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
