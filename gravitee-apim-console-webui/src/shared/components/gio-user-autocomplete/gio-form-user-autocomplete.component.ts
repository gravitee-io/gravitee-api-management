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
import { Component, Input, OnInit, Optional, Self } from '@angular/core';
import { AsyncValidatorFn, ControlValueAccessor, UntypedFormControl, NgControl, ValidationErrors } from '@angular/forms';
import { catchError, debounceTime, distinctUntilChanged, map, share, switchMap, take, takeUntil, tap } from 'rxjs/operators';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { isEmpty } from 'lodash';

import { SearchableUser } from '../../../entities/user/searchableUser';
import { UsersService } from '../../../services-ngx/users.service';

type UserValue = SearchableUser;

@Component({
  selector: 'gio-form-user-autocomplete',
  templateUrl: './gio-form-user-autocomplete.component.html',
  styleUrls: ['./gio-form-user-autocomplete.component.scss'],
  standalone: false,
})
export class GioFormUserAutocompleteComponent implements OnInit, ControlValueAccessor {
  private unsubscribe$: Subject<void> = new Subject<void>();

  @Input()
  private readonly userFilterPredicate: (user: SearchableUser) => boolean = () => true;

  users: Observable<SearchableUser[]>;
  userSearchTerm: UntypedFormControl = new UntypedFormControl('', [], [this.validateSelectedUser()]);

  private _onChange: (value: UserValue) => void = () => ({});

  private _onTouched: () => void = () => ({});

  public isDisabled = false;

  private errors$ = new BehaviorSubject<ValidationErrors>(null);

  constructor(
    private readonly usersService: UsersService,
    @Optional() @Self() public readonly ngControl: NgControl,
  ) {
    // Replace the provider from above with this.
    if (this.ngControl != null) {
      // Setting the value accessor directly (instead of using
      // the providers) to avoid running into a circular import.
      this.ngControl.valueAccessor = this;
    }
  }

  ngOnInit(): void {
    this.users = this.userSearchTerm.valueChanges.pipe(
      distinctUntilChanged(),
      debounceTime(100),
      // Rest form control when value is no more a selected user
      tap(value => {
        if (!isSearchableUser(value)) {
          this._onChange(null);
        }
      }),
      switchMap(term => {
        return term.length > 0 ? this.usersService.search(term) : of([]);
      }),
      map(users =>
        users
          // Filter according to input predicate
          .filter(this.userFilterPredicate),
      ),
      share(),
    );

    this.userSearchTerm.statusChanges.pipe(takeUntil(this.unsubscribe$)).subscribe(() => {
      this._onTouched();
      this.errors$.next(this.userSearchTerm.errors);
    });

    // Add validator to the parent form control
    this.ngControl?.control?.addAsyncValidators(() => {
      return this.errors$.pipe(take(1));
    });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  // From ControlValueAccessor
  writeValue(obj: UserValue): void {
    if (isEmpty(obj)) {
      return;
    }
    if (obj.id) {
      // Only search for user if it has an id
      this.usersService
        .get(obj.id)
        .pipe(
          map(user => {
            this.userSearchTerm.setValue(user.displayName, { emitEvent: false });
          }),
        )
        .subscribe();
    }
  }

  // From ControlValueAccessor
  registerOnChange(fn: any): void {
    this._onChange = fn;
  }

  // From ControlValueAccessor
  registerOnTouched(fn: any): void {
    this._onTouched = fn;
  }

  // From ControlValueAccessor
  setDisabledState(isDisabled: boolean): void {
    this.isDisabled = isDisabled;
    isDisabled ? this.userSearchTerm.disable({ emitEvent: false }) : this.userSearchTerm.enable({ emitEvent: false });
  }

  resetSearchTerm() {
    this.userSearchTerm.setValue('');
  }

  selectUser(event: MatAutocompleteSelectedEvent) {
    const user = event.option.value as SearchableUser;
    this._onChange(user ?? null);
  }

  validateSelectedUser(): AsyncValidatorFn {
    return (control: UntypedFormControl) => {
      // No error if empty. This is handled by required validator
      if (isEmpty(control.value)) {
        return of(null);
      }

      if (!isSearchableUser(control.value)) {
        return of({ invalidUser: true });
      }
      return this.usersService.search(control.value.displayName).pipe(
        map(users => {
          if (users.length === 0) {
            return { invalidUser: true };
          }
          return null;
        }),
        catchError(() => of({ invalidUser: true })),
      );
    };
  }

  displayFn(user: SearchableUser | string): string {
    return isSearchableUser(user) ? user.displayName : user;
  }
}

const isSearchableUser = (user: any): user is SearchableUser => {
  return user && user.displayName;
};
