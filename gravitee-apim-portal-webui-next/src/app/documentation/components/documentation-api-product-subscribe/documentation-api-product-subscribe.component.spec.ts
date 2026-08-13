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
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { Router } from '@angular/router';

import { DocumentationApiProductSubscribeComponent } from './documentation-api-product-subscribe.component';
import { fakeApiProduct } from '../../../../entities/api-product/api-product.fixture';
import { AppTestingModule } from '../../../../testing/app-testing.module';

describe('DocumentationApiProductSubscribeComponent', () => {
  let fixture: ComponentFixture<DocumentationApiProductSubscribeComponent>;
  let router: jest.Mocked<Router>;

  beforeEach(async () => {
    router = { navigate: jest.fn().mockResolvedValue(true) } as unknown as jest.Mocked<Router>;

    await TestBed.configureTestingModule({
      imports: [DocumentationApiProductSubscribeComponent, MatIconTestingModule, AppTestingModule],
      providers: [{ provide: Router, useValue: router }],
      schemas: [NO_ERRORS_SCHEMA],
    })
      .overrideComponent(DocumentationApiProductSubscribeComponent, {
        set: { imports: [MatIconTestingModule], schemas: [NO_ERRORS_SCHEMA] },
      })
      .compileComponents();

    fixture = TestBed.createComponent(DocumentationApiProductSubscribeComponent);
    fixture.componentRef.setInput('apiProduct', fakeApiProduct({ name: 'Developer Product' }));
    fixture.componentRef.setInput('navId', 'nav-id');
    fixture.componentRef.setInput('selectedId', 'selected-id');
    fixture.detectChanges();
  });

  it('should display the API Product name', () => {
    expect(fixture.nativeElement.textContent).toContain('Subscribe to the API Product: Developer Product');
  });

  it('should return to documentation and preserve the selected item', () => {
    fixture.componentInstance.cancel();

    expect(router.navigate).toHaveBeenCalledWith(['/documentation', 'nav-id'], {
      queryParams: { selectedId: 'selected-id' },
    });
  });
});
