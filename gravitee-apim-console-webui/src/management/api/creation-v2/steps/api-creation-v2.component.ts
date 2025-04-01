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
import { Component, ElementRef, Injector, Input, OnDestroy, SimpleChange } from '@angular/core';
import { UpgradeComponent } from '@angular/upgrade/static';
import { combineLatest, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';

import { GroupService } from '../../../../services-ngx/group.service';
import { TenantService } from '../../../../services-ngx/tenant.service';
import { TagService } from '../../../../services-ngx/tag.service';

@Component({
  template: '',
  selector: 'notifications-component',
  standalone: false,
  host: {
    class: 'bootstrap gv-sub-content',
  },
})
export class ApiCreationV2Component extends UpgradeComponent implements OnDestroy {
  @Input() groups;
  @Input() tenants;
  @Input() tags;

  private unsubscribe$ = new Subject<void>();

  constructor(
    elementRef: ElementRef,
    injector: Injector,
    private readonly groupService: GroupService,
    private readonly tenantService: TenantService,
    private readonly tagService: TagService,
    public readonly activatedRoute: ActivatedRoute,
  ) {
    super('apiCreationV2ComponentAjs', elementRef, injector);
  }

  override ngOnInit() {
    combineLatest([this.groupService.list(), this.tenantService.list(), this.tagService.list()])
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(([groups, tenants, tags]) => {
        this.groups = groups;
        this.tenants = tenants;
        this.tags = tags;

        // Hack to Force the binding between Angular and AngularJS
        this.ngOnChanges({
          groups: new SimpleChange(null, this.groups, true),
          tenants: new SimpleChange(null, this.tenants, true),
          tags: new SimpleChange(null, this.tags, true),
          activatedRoute: new SimpleChange(null, this.activatedRoute, true),
        });

        super.ngOnInit();
      });
  }

  override ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();

    super.ngOnDestroy();
  }
}
