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
import { FieldType, FieldTypeConfig } from '@ngx-formly/core';
import { Observable } from 'rxjs';
import { map, startWith, switchMap } from 'rxjs/operators';

import { PlanOAuth2Resource, ResourceTypeService } from './resource-type.service';

@Component({
  selector: 'resource-type',
  templateUrl: './resource-type.component.html',
  styleUrls: ['./resource-type.component.scss'],
})
export class ResourceTypeComponent extends FieldType<FieldTypeConfig> implements OnInit {
  filteredResources: Observable<PlanOAuth2Resource[]>;

  resourceNotExist$: Observable<boolean>;

  constructor(private readonly planOauth2ResourceTypeService: ResourceTypeService) {
    super();
  }

  ngOnInit() {
    if (!this.props.resourceType) {
      // eslint-disable-next-line angular/log
      console.error('ResourceTypeComponent: resourceType is undefined');
      return;
    }

    this.filteredResources = this.formControl.valueChanges.pipe(
      startWith(''),
      switchMap((term) => this.planOauth2ResourceTypeService.filter$(term, this.props.resourceType)),
    );

    this.resourceNotExist$ = this.formControl.valueChanges.pipe(
      switchMap((term) => this.planOauth2ResourceTypeService.getResource(term, this.props.resourceType)),
      map((resource) => !resource),
    );
  }
}
