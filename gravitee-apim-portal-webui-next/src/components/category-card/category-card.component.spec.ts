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
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CategoryCardComponent } from './category-card.component';
import { fakeCategory } from '../../entities/categories/categories.fixture';
import { AppTestingModule } from '../../testing/app-testing.module';

describe('CategoryCardComponent', () => {
  let fixture: ComponentFixture<CategoryCardComponent>;

  const CATEGORY = fakeCategory();

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CategoryCardComponent, AppTestingModule],
      declarations: [],
    }).compileComponents();

    fixture = TestBed.createComponent(CategoryCardComponent);
    fixture.componentRef.setInput('category', CATEGORY);

    fixture.detectChanges();
  });

  it('should display the correct title', () => {
    const titleElement = fixture.nativeElement.querySelector('.m3-title-medium');
    expect(titleElement.textContent).toContain(CATEGORY.name);
  });

  it('should display content if available', () => {
    const contentElement = fixture.nativeElement.querySelector('.app-card__description');
    expect(contentElement).not.toBeNull();
    if (contentElement) {
      expect(contentElement.textContent).toContain(CATEGORY.description);
    }
  });

  it('should display the picture component with the correct inputs', () => {
    const pictureComponent = fixture.nativeElement.querySelector('app-picture');
    expect(pictureComponent.getAttribute('ng-reflect-picture')).toEqual(CATEGORY._links?.picture);
    expect(pictureComponent.getAttribute('ng-reflect-hash-value')).toEqual(`${CATEGORY.name}`);
  });
});
