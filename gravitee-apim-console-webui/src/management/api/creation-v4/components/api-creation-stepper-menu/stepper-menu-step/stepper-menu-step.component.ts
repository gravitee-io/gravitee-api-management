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
import { Component, EventEmitter, Injector, Input, OnChanges, Output, Type } from '@angular/core';

import { MenuStepItem, MENU_ITEM_PAYLOAD } from '../api-creation-stepper-menu.component';

@Component({
  selector: 'stepper-menu-step',
  templateUrl: './stepper-menu-step.component.html',
  styleUrls: ['./stepper-menu-step.component.scss'],
  standalone: false,
})
export class StepperMenuStepComponent implements OnChanges {
  @Input()
  public stepNumber: number;

  // Step item (primary or secondary)
  @Input()
  public step: MenuStepItem;

  // Set to true if the step is the current edited step
  @Input()
  public activeStep: boolean;

  @Output()
  goToStep = new EventEmitter<string>();

  public stepStatus: 'INACTIVE' | 'ACTIVE' | 'FILLED' | 'INVALID';

  public menuItemComponentInjector: Injector;

  private getStepStatus() {
    if (this.activeStep) return 'ACTIVE';
    if (this.step.state === 'valid') return 'FILLED';
    if (this.step.state === 'invalid') return 'INVALID';
    return 'INACTIVE';
  }

  public previewOutlet?: {
    component: Type<unknown>;
    injector: Injector;
  };

  public openPreview = false;
  public clickable = false;

  constructor(private readonly injector: Injector) {}

  ngOnChanges(): void {
    this.stepStatus = this.getStepStatus();

    if (this.step.menuItemComponent) {
      this.previewOutlet = {
        component: this.step.menuItemComponent,
        injector: Injector.create({
          providers: [{ provide: MENU_ITEM_PAYLOAD, useValue: this.step.payload }],
          parent: this.injector,
        }),
      };
    }

    this.clickable = !this.activeStep && this.step.state === 'valid';
    this.openPreview = this.activeStep && this.step.state === 'valid';
  }

  emitGoToStep() {
    this.goToStep.emit(this.step.label);
  }
}
