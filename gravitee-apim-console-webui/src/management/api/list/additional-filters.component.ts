import { CommonModule } from "@angular/common";
import { Component, Inject, OnInit } from "@angular/core";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { BehaviorSubject } from "rxjs";

import { ColumnType, FilterType } from "./api-list.component";

@Component({
    selector: 'api-list-additional-filters',
    template: `
<mat-form-field style='margin-left: 8px;'>
  <mat-label>API Type</mat-label>
  <mat-select class="portal__form__card__form-field" [multiple]="true" [(value)]="checkedApiTypes"
    (selectionChange)="notifyChange(FilterType.API_TYPE)">
    <mat-option value="2.0.0">V2</mat-option>
    <mat-option value="4.0.0">V4</mat-option>
  </mat-select>
  </mat-form-field>

<mat-form-field>
  <mat-label>Status</mat-label>
  <mat-select class="portal__form__card__form-field" [multiple]="true" [(value)]="checkedStatuses"
    (selectionChange)="notifyChange(FilterType.STATUS)">
    <mat-option value="STARTED">Started</mat-option>
    <mat-option value="STOPPED">Stopped</mat-option>
  </mat-select>
  </mat-form-field>

  <mat-form-field>
  <mat-label>Sharding Tags</mat-label>
  <mat-select class="portal__form__card__form-field" [multiple]="true" [(value)]="checkedTags"
    (selectionChange)="notifyChange(FilterType.TAGS)">
    <mat-option *ngFor="let tag of tags" [value]="tag">{{ tag }}</mat-option>
  </mat-select>
  </mat-form-field>

  <mat-form-field>
  <mat-label>Categories</mat-label>
  <mat-select class="portal__form__card__form-field" [multiple]="true" [(value)]="checkedCategories"
    (selectionChange)="notifyChange(FilterType.CATEGORIES)">
    <mat-option *ngFor="let category of categories | keyvalue" [value]="category.key">{{ category.value }}</mat-option>
  </mat-select>
  </mat-form-field>

  <mat-form-field>
  <mat-label>Published</mat-label>
  <mat-select class="portal__form__card__form-field" [multiple]="true" [(value)]="checkedPublished"
    (selectionChange)="notifyChange(FilterType.PUBLISHED)">
    <mat-option value="PUBLISHED">Published</mat-option>
    <mat-option value="UNPUBLISHED">Unpublished</mat-option>
  </mat-select>
  </mat-form-field>

  <mat-form-field>
  <mat-label>{{ visibleColumns.length == 0 ? 'Columns' : '' }}</mat-label>
  <mat-select class="" [multiple]="true" [(value)]="visibleColumns"
    (selectionChange)="updateVisibleColumns()">
    <mat-select-trigger>
      Columns
    </mat-select-trigger>
    <mat-option value="definitionVersion">API Type</mat-option>
    <mat-option value="states">Status</mat-option>
    <mat-option value="access">Access</mat-option>
    <mat-option value="tags">Tags</mat-option>
    <mat-option value="categories">Categories</mat-option>
    <mat-option value="owner">Owner</mat-option>
    <mat-option value="visibility">Visibility</mat-option>
  </mat-select>
  </mat-form-field>
    `,
    styles: [ `
mat-form-field {
    margin-right: 8px;
    width: 150px;
}`
    ],
    imports: [MatInputModule, MatSelectModule, CommonModule]
  })
export class ApiListAdditionalFiltersComponent implements OnInit {

    FilterType = FilterType;
    ColumnType = ColumnType;

    apiTypes: string[] = [ 'V2', 'V4' ];
    statuses: any[] = [ 'STARTED', 'STOPPED' ];
    tags: string[] = [];
    categories: Map<string, string> = new Map<string, string>();
    published: string[] = [ 'PUBLISHED', 'UNPUBLISHED' ];

    checkedApiTypes: string[];
    checkedStatuses: string[];
    checkedTags: string[];
    checkedCategories: string[];
    checkedPublished: string[];

    visibleColumns: string[];

    constructor(
        @Inject('APIS_TAGS') private apisTags$: BehaviorSubject<string[]>,
        @Inject('APIS_CATEGORIES') private apisCategories$: BehaviorSubject<Map<string, string>>,
        @Inject('UPDATE_VISIBLE_COLUMNS') private updateVisibleColumns$: BehaviorSubject<string[]>,
        @Inject('CALLBACK_FROM_A') private notifyA: (data: any) => void
      ) {
        this.visibleColumns = updateVisibleColumns$.getValue();
      }

      ngOnInit() {
        this.apisTags$.subscribe(tags => {
          this.tags = tags;
        });
        this.apisCategories$.subscribe(categories => this.categories = categories);
      }
    
      notifyChange(status) {
        let value;
        switch (status) {
            case FilterType.API_TYPE:
                value = this.checkedApiTypes;
                break;
            case FilterType.STATUS:
                value = this.checkedStatuses;
                break;
            case FilterType.TAGS:
                value = this.checkedTags;
                break;
            case FilterType.CATEGORIES:
                value = this.checkedCategories;
                break;
            case FilterType.PUBLISHED:
                value = this.checkedPublished;
                break;
        }
        this.notifyA({type: status, value: value});
      }

      updateVisibleColumns() {
        const visibleColumns = ['picture', 'name', ...this.visibleColumns, 'actions' ];
        this.updateVisibleColumns$.next(visibleColumns);
      }
}