/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { CdkAccordionModule } from '@angular/cdk/accordion';
import { NgModule } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

import { AccordionTitleComponent } from './accordion-title.component';
import { AccordionComponent } from './accordion.component';

@NgModule({
  declarations: [AccordionTitleComponent, AccordionComponent],
  exports: [AccordionTitleComponent, AccordionComponent],
  imports: [CdkAccordionModule, MatIconModule],
})
export class AccordionModule {}
