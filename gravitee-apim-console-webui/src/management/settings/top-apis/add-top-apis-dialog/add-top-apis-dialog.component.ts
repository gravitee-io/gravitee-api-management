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

import { Component, Inject, OnInit, DestroyRef, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormControl } from '@angular/forms';
import { forkJoin, Observable } from 'rxjs';
import { filter, map, startWith, tap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter as lodashFilter, includes as lodashIncludes, map as lodashMap } from 'lodash';

import { TopApi } from '../top-apis.model';
import { ApiService } from '../../../../services-ngx/api.service';
import { Api } from '../../../../entities/api';
import { TopApiService } from '../../../../services-ngx/top-api.service';

export interface AddTopApisDialogData {
  title: string;
}
export type AddTopApisDialogResult = Api;

@Component({
  selector: 'add-top-apis-dialog',
  templateUrl: './add-top-apis-dialog.component.html',
  styleUrls: ['./add-top-apis-dialog.component.scss'],
})
export class AddTopApisDialogComponent implements OnInit {
  public searchApiControl: FormControl<string | Api> = new FormControl('');
  public apis: Api[] = [];
  public filteredOptions$: Observable<Api[]>;
  public isApiSelected = false;
  private destroyRef = inject(DestroyRef);

  constructor(
    public dialogRef: MatDialogRef<AddTopApisDialogComponent>,
    private apiService: ApiService,
    private topApiService: TopApiService,
    @Inject(MAT_DIALOG_DATA) public data: AddTopApisDialogData,
  ) {}

  ngOnInit(): void {
    forkJoin([this.apiService.getAll(), this.topApiService.getList()])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(([apis, topApis]: [Api[], TopApi[]]) => {
        this.apis = this.removeTopApis(apis, topApis);
      });

    this.filteredOptions$ = this.searchApiControl.valueChanges.pipe(
      startWith(''),
      filter((v) => typeof v === 'string'),
      tap(() => (this.isApiSelected = false)),
      map((value: string): Api[] => (value ? this._filter(value) : [])),
      takeUntilDestroyed(this.destroyRef),
    );
  }

  removeTopApis(apis, topApis): Api[] {
    return lodashFilter(apis, (api: Api) => !lodashIncludes(lodashMap(topApis, 'api'), api.id));
  }

  private _filter(value: string): Api[] {
    const filterValue = value.toLowerCase();
    return this.apis.filter((api: Api) => {
      return api.name.toLowerCase().includes(filterValue) || api.description.toLowerCase().includes(filterValue);
    });
  }

  public displayFn(option: Api): string {
    if (option && option.name && option.version) {
      return option.name + ' - ' + option.version;
    }
    return option.toString();
  }

  resetSearchTerm() {
    this.searchApiControl.setValue('');
  }

  onCancelClick(): void {
    this.dialogRef.close();
  }

  selectTopAPI(): void {
    this.isApiSelected = true;
  }

  public submit() {
    this.dialogRef.close(this.searchApiControl.getRawValue());
  }
}
