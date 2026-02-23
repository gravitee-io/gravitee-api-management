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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PaginationComponent } from './pagination.component';
import { PaginationHarness } from './pagination.harness';

describe('PaginationComponent', () => {
  let component: PaginationComponent;
  let fixture: ComponentFixture<PaginationComponent>;
  let componentHarness: PaginationHarness;
  let selectPageSpy: jest.SpyInstance;

  const init = async (currentPage: number, totalResults: number) => {
    await TestBed.configureTestingModule({
      imports: [PaginationComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PaginationComponent);

    fixture.componentRef.setInput('currentPage', currentPage);
    fixture.componentRef.setInput('totalResults', totalResults);

    component = fixture.componentInstance;
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, PaginationHarness);
    selectPageSpy = jest.spyOn(component.selectPage, 'emit');
    fixture.detectChanges();
  };

  describe('Only one page of results', () => {
    beforeEach(async () => {
      await init(1, 5);
    });

    it('should not allow previous page on load', async () => {
      const previousPageButton = await componentHarness.getPreviousPageButton();
      expect(previousPageButton).toBeTruthy();
      expect(await previousPageButton.isDisabled()).toEqual(true);
    });
    it('should not allow next page when on last page', async () => {
      const nextPageButton = await componentHarness.getNextPageButton();
      expect(nextPageButton).toBeTruthy();
      expect(await nextPageButton.isDisabled()).toEqual(true);
    });
    it('should highlight current page', async () => {
      const currentPaginationPage = await componentHarness.getCurrentPaginationPage();
      expect(currentPaginationPage).toBeTruthy();
      expect(await currentPaginationPage.getText()).toEqual('1');
    });
  });

  describe('First of many pages', () => {
    beforeEach(async () => {
      await init(1, 79);
    });

    it('should not allow previous page on load', async () => {
      const previousPageButton = await componentHarness.getPreviousPageButton();
      expect(previousPageButton).toBeTruthy();
      expect(await previousPageButton.isDisabled()).toEqual(true);
    });
    it('should allow next page', async () => {
      const nextPageButton = await componentHarness.getNextPageButton();
      expect(nextPageButton).toBeTruthy();
      expect(await nextPageButton.isDisabled()).toEqual(false);
    });
    it('should show "2" for next page', async () => {
      const secondPageButton = await componentHarness.getPageButtonByNumber(2);
      expect(secondPageButton).toBeTruthy();
      expect(await secondPageButton.isDisabled()).toEqual(false);
    });
    it('should show "3" for page option', async () => {
      const thirdPageButton = await componentHarness.getPageButtonByNumber(3);
      expect(thirdPageButton).toBeTruthy();
      expect(await thirdPageButton.isDisabled()).toEqual(false);
    });

    it('should show "8" for last page', async () => {
      const lastPageButton = await componentHarness.getPageButtonByNumber(8);
      expect(lastPageButton).toBeTruthy();
      expect(await lastPageButton.isDisabled()).toEqual(false);
    });

    it('should highlight current page', async () => {
      const currentPaginationPage = await componentHarness.getCurrentPaginationPage();
      expect(currentPaginationPage).toBeTruthy();
      expect(await currentPaginationPage.getText()).toEqual('1');
    });
    it('should go to next page via page number button', async () => {
      const secondPageButton = await componentHarness.getPageButtonByNumber(2);
      await secondPageButton.click();
      fixture.detectChanges();
    });
    it('should go to last page', async () => {
      const lastPageButton = await componentHarness.getPageButtonByNumber(8);
      await lastPageButton.click();
      expect(selectPageSpy).toHaveBeenCalledWith(8);
    });
  });

  describe('Third page of many pages of results', () => {
    beforeEach(async () => {
      await init(3, 79);
    });

    it('should allow previous page', async () => {
      const previousPageButton = await componentHarness.getPreviousPageButton();
      expect(previousPageButton).toBeTruthy();
      expect(await previousPageButton.isDisabled()).toEqual(false);
    });
    it('should allow next page', async () => {
      const nextPageButton = await componentHarness.getNextPageButton();
      expect(nextPageButton).toBeTruthy();
      expect(await nextPageButton.isDisabled()).toEqual(false);
    });
    it('should show "1" for first page', async () => {
      const firstPageButton = await componentHarness.getPageButtonByNumber(1);
      expect(firstPageButton).toBeTruthy();
      expect(await firstPageButton.isDisabled()).toEqual(false);
    });
    it('should show "2" for previous page', async () => {
      const secondPageButton = await componentHarness.getPageButtonByNumber(2);
      expect(secondPageButton).toBeTruthy();
      expect(await secondPageButton.isDisabled()).toEqual(false);
    });
    it('should show "8" for last page', async () => {
      const lastPageButton = await componentHarness.getPageButtonByNumber(8);
      expect(lastPageButton).toBeTruthy();
      expect(await lastPageButton.isDisabled()).toEqual(false);
    });
    it('should highlight current page', async () => {
      const currentPaginationPage = await componentHarness.getCurrentPaginationPage();
      expect(currentPaginationPage).toBeTruthy();
      expect(await currentPaginationPage.getText()).toEqual('3');
    });
    it('should go to previous page via arrow', async () => {
      const previousPageButton = await componentHarness.getPreviousPageButton();
      await previousPageButton.click();

      expect(selectPageSpy).toHaveBeenCalledWith(2);
    });
  });

  describe('Previous and Next button labels', () => {
    beforeEach(async () => {
      await init(2, 30);
    });

    it('should show "Previous" text on previous button', async () => {
      const previousPageButton = await componentHarness.getPreviousPageButton();
      expect(await previousPageButton.getText()).toContain('Previous');
    });

    it('should show "Next" text on next button', async () => {
      const nextPageButton = await componentHarness.getNextPageButton();
      expect(await nextPageButton.getText()).toContain('Next');
    });

    it('should enable Previous button when not on first page', async () => {
      const previousPageButton = await componentHarness.getPreviousPageButton();
      expect(await previousPageButton.isDisabled()).toEqual(false);
    });

    it('should enable Next button when not on last page', async () => {
      const nextPageButton = await componentHarness.getNextPageButton();
      expect(await nextPageButton.isDisabled()).toEqual(false);
    });
  });
});
