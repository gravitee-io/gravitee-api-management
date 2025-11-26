import { Component, effect, input, OnInit, output } from '@angular/core';
import { distinctUntilChanged, Observable } from 'rxjs';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { DropdownSearchComponent, ResultsLoaderInput, ResultsLoaderOutput } from '../dropdown-search/dropdown-search.component';
import { SelectOption } from '../dropdown-search/dropdown-search-overlay/dropdown-search-overlay.component';
import { AsyncPipe, JsonPipe } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';

export interface SelectedFilter {
  parentKey: string;
  value: string;
}

export interface Filter {
  key: string;
  label: string;
  data?: SelectOption[];
  data$?: Observable<SelectOption[]>;
  dataLoader?: (input: ResultsLoaderInput) => Observable<ResultsLoaderOutput>;
}

@Component({
  selector: 'gd-generic-filter-bar',
  imports: [DropdownSearchComponent, AsyncPipe, ReactiveFormsModule, MatFormFieldModule, MatSelectModule, JsonPipe],
  templateUrl: './generic-filter-bar.component.html',
  styleUrl: './generic-filter-bar.component.scss',
})
export class GenericFilterBarComponent implements OnInit {
  filters = input.required<Filter[]>();
  // selectedFilter = output<SelectedFilter>()
  currentSelectedFilters = input.required<SelectedFilter[]>();

  selectedFilters = output<SelectedFilter[]>();

  form = new FormGroup<Record<string, FormControl<string[]>>>({});

  // For each filter, add a control to manage the selected option
  // When form changes, emit the value to the parent component

  constructor() {
    effect(() => {
      this.filters().forEach(filter => {
        if (!this.form.contains(filter.key)) {
          this.form.addControl(filter.key, new FormControl<string[]>([], { nonNullable: true }));
        }
      });
    });

    effect(() => {
      // For each current selected filter, set the value of the corresponding control
      this.currentSelectedFilters().forEach(selectedFilter => {
        const control = this.form.get(selectedFilter.parentKey);
        if (control) {
          const currentValues = control.value;
          if (!currentValues.includes(selectedFilter.value)) {
            control.setValue([...currentValues, selectedFilter.value]);
          }
        }
      });
    });
  }

  ngOnInit(): void {
    this.form.valueChanges.pipe(distinctUntilChanged()).subscribe(value => {
      console.log('value changed', value);
      const selected: SelectedFilter[] = [];
      for (const key in value) {
        const selectedOptions = value[key];
        if (selectedOptions) {
          selectedOptions.forEach(option => {
            selected.push({ parentKey: key, value: option });
          });
        }
      }
      this.selectedFilters.emit(selected);
    });
  }
}
