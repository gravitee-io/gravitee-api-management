/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
    it('should highlight current page', async () => {
      const currentPaginationPage = await componentHarness.getCurrentPaginationPage();
      expect(currentPaginationPage).toBeTruthy();
      expect(await currentPaginationPage.getText()).toEqual('1');
    });
    it('should go to next page via Next button', async () => {
      const nextPageButton = await componentHarness.getNextPageButton();
      await nextPageButton.click();
      fixture.detectChanges();
      expect(selectPageSpy).toHaveBeenCalledWith(2);
    });
    it('should go to page 2 when clicking page number 2', async () => {
      const page2Button = await componentHarness.getPageNumberButton(2);
      await page2Button.click();
      expect(selectPageSpy).toHaveBeenCalledWith(2);
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

  describe('Last page of many pages', () => {
    beforeEach(async () => {
      await init(8, 79);
    });

    it('should show page window adjusted to end', async () => {
      const currentPaginationPage = await componentHarness.getCurrentPaginationPage();
      expect(await currentPaginationPage.getText()).toEqual('8');
    });

    it('should not allow next page on last page', async () => {
      const nextPageButton = await componentHarness.getNextPageButton();
      expect(await nextPageButton.isDisabled()).toEqual(true);
    });

    it('should not go to next page when already on last page', async () => {
      component.goToNextPage();
      expect(selectPageSpy).not.toHaveBeenCalled();
    });
  });

  describe('Zero results', () => {
    beforeEach(async () => {
      await init(1, 0);
    });

    it('should return empty page numbers', () => {
      expect(component.pageNumbers()).toEqual([]);
    });
  });

  describe('Page size options', () => {
    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [PaginationComponent],
      }).compileComponents();

      fixture = TestBed.createComponent(PaginationComponent);
      fixture.componentRef.setInput('currentPage', 1);
      fixture.componentRef.setInput('totalResults', 50);
      fixture.componentRef.setInput('pageSizeOptions', [5, 10, 25]);

      component = fixture.componentInstance;
      selectPageSpy = jest.spyOn(component.selectPage, 'emit');
      fixture.detectChanges();
    });

    it('should emit pageSizeChange on page size change', () => {
      const pageSizeChangeSpy = jest.spyOn(component.pageSizeChange, 'emit');
      component.onPageSizeChange(25);
      expect(pageSizeChangeSpy).toHaveBeenCalledWith(25);
    });
  });

  describe('Harness error case', () => {
    beforeEach(async () => {
      await init(1, 5);
    });

    it('should throw error when page number button not found', async () => {
      await expect(componentHarness.getPageNumberButton(999)).rejects.toThrow('Page 999 button not found');
    });
  });
});
