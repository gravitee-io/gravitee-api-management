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
import { ComponentFactory, ViewContainerRef } from '@angular/core';

export abstract class GvSlot {
  private currentComponentFactory: ComponentFactory<any>;

  constructor(private viewContainerRef: ViewContainerRef) {}

  abstract getName(): string;

  getViewContainerRef(): ViewContainerRef {
    return this.viewContainerRef;
  }

  clear() {
    this.currentComponentFactory = null;
    this.viewContainerRef.clear();
  }

  setComponent(componentFactory: ComponentFactory<any>) {
    if (this.currentComponentFactory) {
      if (this.currentComponentFactory.componentType !== componentFactory.componentType) {
        this.clear();
        this.createComponent(componentFactory);
      }
    } else {
      this.createComponent(componentFactory);
    }
  }

  private createComponent(componentFactory) {
    this.viewContainerRef.createComponent(componentFactory);
    if (this.viewContainerRef.element.nativeElement.previousSibling) {
      this.viewContainerRef.element.nativeElement.previousSibling.slot = this.getName();
    }
    this.currentComponentFactory = componentFactory;
  }
}
