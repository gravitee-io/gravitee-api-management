/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { Constants } from '../../../entities/Constants';

@Component({
  selector: 'api-product-configuration',
  templateUrl: './api-product-configuration.component.html',
  styleUrls: ['./api-product-configuration.component.scss'],
  standalone: false,
})
export class ApiProductConfigurationComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  public form: UntypedFormGroup;
  public descriptionMaxLength = 250;
  public apiProductId: string;
  public apiProduct: any; // TODO: Replace with proper ApiProduct type

  constructor(
    @Inject(Constants) private readonly constants: Constants,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly formBuilder: UntypedFormBuilder,
  ) {}

  ngOnInit(): void {
    // Get apiProductId from route params (check parent route if not in current route)
    const getApiProductId = (route: ActivatedRoute): string | null => {
      if (route.snapshot.params['apiProductId']) {
        return route.snapshot.params['apiProductId'];
      }
      if (route.parent) {
        return getApiProductId(route.parent);
      }
      return null;
    };

    this.apiProductId = getApiProductId(this.activatedRoute) || '';
    
    // TODO: Load API Product from service
    // For now, create a mock object
    this.apiProduct = {
      id: this.apiProductId,
      name: 'My API Product',
      version: '1',
      description: '',
    };

    this.form = this.formBuilder.group({
      name: this.formBuilder.control(this.apiProduct.name || '', [Validators.required]),
      version: this.formBuilder.control(this.apiProduct.version || '', [Validators.required]),
      description: this.formBuilder.control(this.apiProduct.description || '', [
        Validators.maxLength(this.descriptionMaxLength),
      ]),
    });
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  onSubmit(): void {
    if (this.form.valid) {
      // TODO: Update API Product via service
      console.log('Updating API Product:', this.form.value);
    } else {
      this.form.markAllAsTouched();
    }
  }

  getDescriptionLength(): number {
    return this.form.get('description')?.value?.length || 0;
  }
}

