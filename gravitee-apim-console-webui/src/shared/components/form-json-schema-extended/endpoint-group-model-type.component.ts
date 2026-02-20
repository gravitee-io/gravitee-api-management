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
import { Observable, of } from 'rxjs';
import { startWith, switchMap } from 'rxjs/operators';

import { EndpointGroupTypeService, ModelGroup } from './endpoint-group-type.service';

@Component({
  selector: 'endpoint-group-model-type',
  templateUrl: './endpoint-group-model-type.component.html',
  styleUrls: ['./endpoint-group-model-type.component.scss'],
  standalone: false,
})
export class EndpointGroupModelTypeComponent extends FieldType<FieldTypeConfig> implements OnInit {
  filteredModels: Observable<string[]>;
  filteredModelGroups: Observable<ModelGroup[]>;
  aggregatedMode = false;

  constructor(private readonly endpointGroupTypeService: EndpointGroupTypeService) {
    super();
  }

  ngOnInit() {
    const endpointGroupType = this.props.endpointGroupType;
    if (endpointGroupType) {
      this.initAggregatedMode(endpointGroupType);
    } else {
      this.initLegacyMode();
    }
  }

  private initAggregatedMode(type: string): void {
    this.aggregatedMode = true;

    this.filteredModelGroups = this.formControl.valueChanges.pipe(
      startWith(this.formControl.value ?? ''),
      switchMap((term) => this.endpointGroupTypeService.filterModelGroupsByType$(type, term)),
    );

    // Auto-resolve the endpoint group when a model is selected
    const endpointGroupField = this.props.endpointGroupField;
    const groupControl = endpointGroupField ? this.form.get(endpointGroupField) : null;
    if (groupControl) {
      this.formControl.valueChanges.pipe(startWith(this.formControl.value ?? '')).subscribe((modelName) => {
        const resolvedGroup = this.endpointGroupTypeService.findGroupForModel(type, modelName);
        if (resolvedGroup) {
          groupControl.setValue(resolvedGroup, { emitEvent: false });
        }
      });
    }
  }

  private initLegacyMode(): void {
    const endpointGroupField = this.props.endpointGroupField;
    if (!endpointGroupField) {
      // eslint-disable-next-line angular/log
      console.error('EndpointGroupModelTypeComponent: endpointGroupField is undefined');
      return;
    }

    const groupControl = this.form.get(endpointGroupField);
    if (!groupControl) {
      // eslint-disable-next-line angular/log
      console.error(`EndpointGroupModelTypeComponent: form control '${endpointGroupField}' not found`);
      return;
    }

    // Observe the sibling endpoint group field to reactively update model options
    const groupName$ = groupControl.valueChanges.pipe(startWith(groupControl.value ?? ''));

    this.filteredModels = groupName$.pipe(
      switchMap((groupName) => {
        if (!groupName) {
          return of([]);
        }
        return this.formControl.valueChanges.pipe(
          startWith(this.formControl.value ?? ''),
          switchMap((term) => this.endpointGroupTypeService.filterModels$(groupName, term)),
        );
      }),
    );
  }
}
