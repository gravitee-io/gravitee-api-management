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
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { includes } from 'lodash';
import { combineLatest, Subject } from 'rxjs';
import { map, startWith, takeUntil } from 'rxjs/operators';

import { UIRouterStateParams } from '../../../../../../../ajs-upgraded-providers';
import { ApiService } from '../../../../../../../services-ngx/api.service';
import { CurrentUserService } from '../../../../../../../services-ngx/current-user.service';
import { DocumentationService } from '../../../../../../../services-ngx/documentation.service';
import { GroupService } from '../../../../../../../services-ngx/group.service';
import { TagService } from '../../../../../../../services-ngx/tag.service';

@Component({
  selector: 'plan-edit-general-step',
  template: require('./plan-edit-general-step.component.html'),
  styles: [require('./plan-edit-general-step.component.scss')],
})
export class PlanEditGeneralStepComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  generalForm: FormGroup;

  conditionPages$ = this.documentationService.apiSearch(this.ajsStateParams.apiId, {
    type: 'MARKDOWN',
    api: this.ajsStateParams.apiId,
  });
  shardingTags$ = combineLatest([
    this.tagService.list(),
    this.apiService.get(this.ajsStateParams.apiId),
    this.currentUserService.getTags(),
  ]).pipe(
    map(([tags, api, userTags]) => {
      return tags.map((tag) => ({
        ...tag,
        disabled: !includes(userTags, tag.id) || !includes(api.tags, tag.id),
      }));
    }),
  );

  groups$ = this.groupService.list();

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    private readonly tagService: TagService,
    private readonly groupService: GroupService,
    private readonly documentationService: DocumentationService,
    private readonly currentUserService: CurrentUserService,
    private readonly apiService: ApiService,
  ) {}

  ngOnInit(): void {
    this.generalForm = new FormGroup({
      name: new FormControl('', [Validators.required, Validators.maxLength(50)]),
      description: new FormControl(''),
      characteristics: new FormControl([]),
      generalConditions: new FormControl(''),
      validation: new FormControl(false),
      commentRequired: new FormControl(false),
      commentMessage: new FormControl(''),
      shardingTags: new FormControl([]),
      excludedGroups: new FormControl([]),
    });

    // Enable comment message only if comment required is checked
    this.generalForm
      .get('commentRequired')
      .valueChanges.pipe(takeUntil(this.unsubscribe$), startWith(this.generalForm.get('commentRequired').value))
      .subscribe((value) => {
        value ? this.generalForm.get('commentMessage').enable() : this.generalForm.get('commentMessage').disable();
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
