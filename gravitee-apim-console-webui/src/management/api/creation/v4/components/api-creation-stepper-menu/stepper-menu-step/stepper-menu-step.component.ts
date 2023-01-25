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
import { Component, Injector, Input, OnChanges } from '@angular/core';

import { MenuStepItem, MENU_ITEM_PAYLOAD } from '../api-creation-stepper-menu.component';

@Component({
  selector: 'stepper-menu-step',
  template: require('./stepper-menu-step.component.html'),
  styles: [require('./stepper-menu-step.component.scss')],
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

  public stepStatus: 'INACTIVE' | 'ACTIVE' | 'FILLED';

  public menuItemComponentInjector: Injector;

  private getStepStatus() {
    if (this.activeStep) return 'ACTIVE';
    if (this.step.state === 'valid') return 'FILLED';
    return 'INACTIVE';
  }

  constructor(private readonly injector: Injector) {}

  ngOnChanges(): void {
    this.stepStatus = this.getStepStatus();

    this.menuItemComponentInjector = Injector.create({
      providers: [{ provide: MENU_ITEM_PAYLOAD, useValue: this.step.payload }],
      parent: this.injector,
    });
  }
}
