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
import { map, startWith, switchMap } from 'rxjs/operators';

import { McpProxyApiEntry, McpProxyApiTypeService } from './mcp-proxy-api-type.service';

@Component({
  selector: 'mcp-proxy-api-type',
  templateUrl: './mcp-proxy-api-type.component.html',
  styleUrls: ['./mcp-proxy-api-type.component.scss'],
  standalone: false,
})
export class McpProxyApiTypeComponent extends FieldType<FieldTypeConfig> implements OnInit {
  filteredApis: Observable<McpProxyApiEntry[]>;
  apiNotExist$: Observable<boolean>;
  displayFn: (id: string) => string;

  constructor(private readonly mcpProxyApiTypeService: McpProxyApiTypeService) {
    super();
    this.displayFn = (id: string) => this.mcpProxyApiTypeService.getApiName(id);
  }

  ngOnInit() {
    // Load APIs eagerly so displayFn can resolve names synchronously
    this.mcpProxyApiTypeService.loadApis$().subscribe();

    this.filteredApis = this.formControl.valueChanges.pipe(
      startWith(this.formControl.value ?? ''),
      switchMap((term) => {
        // If the value is an API ID (already selected), show all APIs
        if (this.mcpProxyApiTypeService.apiExists(term)) {
          return this.mcpProxyApiTypeService.filterApis$(undefined);
        }
        return this.mcpProxyApiTypeService.filterApis$(term);
      }),
    );

    this.apiNotExist$ = this.formControl.valueChanges.pipe(
      startWith(this.formControl.value ?? ''),
      switchMap((value) => {
        if (!value) {
          return of(false);
        }
        return this.mcpProxyApiTypeService.loadApis$().pipe(map(() => !this.mcpProxyApiTypeService.apiExists(value)));
      }),
    );
  }
}
