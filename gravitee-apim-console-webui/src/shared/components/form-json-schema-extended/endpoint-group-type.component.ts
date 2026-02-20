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

import { EndpointGroupEntry, EndpointGroupTypeService } from './endpoint-group-type.service';

@Component({
  selector: 'endpoint-group-type',
  templateUrl: './endpoint-group-type.component.html',
  styleUrls: ['./endpoint-group-type.component.scss'],
  standalone: false,
})
export class EndpointGroupTypeComponent extends FieldType<FieldTypeConfig> implements OnInit {
  filteredGroups: Observable<EndpointGroupEntry[]>;

  groupNotExist$: Observable<boolean>;

  constructor(private readonly endpointGroupTypeService: EndpointGroupTypeService) {
    super();
  }

  ngOnInit() {
    if (!this.props.endpointGroupType) {
      // eslint-disable-next-line angular/log
      console.error('EndpointGroupTypeComponent: endpointGroupType is undefined');
      return;
    }

    this.filteredGroups = this.formControl.valueChanges.pipe(
      startWith(''),
      switchMap((term) => this.endpointGroupTypeService.filter$(term, this.props.endpointGroupType)),
    );

    this.groupNotExist$ = this.formControl.valueChanges.pipe(
      switchMap((term) => this.endpointGroupTypeService.getEndpointGroup(term, this.props.endpointGroupType)),
      map((group) => !group),
    );
  }
}
