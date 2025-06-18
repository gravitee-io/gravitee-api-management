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

import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { distinctUntilChanged } from 'rxjs/operators';

export enum ScreenSize {
  XS = 'xs', // 0-480px
  SM = 'sm', // 481-768px
  MD = 'md', // 769-1024px
  LG = 'lg', // 1025-1280px
  XL = 'xl', // 1281px+
}

export interface ScreenSizeInfo {
  size: ScreenSize;
  width: number;
  height: number;
  isMobile: boolean;
  isTablet: boolean;
  isDesktop: boolean;
}

const breakpoints = {
  xs: 480,
  sm: 768,
  md: 1024,
  lg: 1280,
  xl: 1440,
};

@Injectable({
  providedIn: 'root',
})
export class ResponsiveService {
  public screenSizeSubject = new BehaviorSubject<ScreenSizeInfo>(this.getScreenSizeInfo());
  public screenSize$: Observable<ScreenSizeInfo> = this.screenSizeSubject
    .asObservable()
    .pipe(distinctUntilChanged((prev, curr) => prev.size === curr.size));

  constructor() {
    this.initializeResizeListener();
  }

  // Public methods for components to use
  getCurrentScreenSize(): ScreenSizeInfo {
    return this.screenSizeSubject.value;
  }

  isMobile(): boolean {
    return this.getCurrentScreenSize().isMobile;
  }

  isTablet(): boolean {
    return this.getCurrentScreenSize().isTablet;
  }

  isDesktop(): boolean {
    return this.getCurrentScreenSize().isDesktop;
  }

  isScreenSize(size: ScreenSize): boolean {
    return this.getCurrentScreenSize().size === size;
  }

  isScreenSizeOrSmaller(size: ScreenSize): boolean {
    const currentSize = this.getCurrentScreenSize().size;
    const sizeOrder = [ScreenSize.XS, ScreenSize.SM, ScreenSize.MD, ScreenSize.LG, ScreenSize.XL];
    const currentIndex = sizeOrder.indexOf(currentSize);
    const targetIndex = sizeOrder.indexOf(size);
    return currentIndex <= targetIndex;
  }

  isScreenSizeOrLarger(size: ScreenSize): boolean {
    const currentSize = this.getCurrentScreenSize().size;
    const sizeOrder = [ScreenSize.XS, ScreenSize.SM, ScreenSize.MD, ScreenSize.LG, ScreenSize.XL];
    const currentIndex = sizeOrder.indexOf(currentSize);
    const targetIndex = sizeOrder.indexOf(size);
    return currentIndex >= targetIndex;
  }

  private initializeResizeListener(): void {
    if (typeof window !== 'undefined') {
      window.addEventListener('resize', () => {
        this.screenSizeSubject.next(this.getScreenSizeInfo());
      });
    }
  }

  private getScreenSizeInfo(): ScreenSizeInfo {
    if (typeof window === 'undefined') {
      return this.getDefaultScreenSizeInfo();
    }

    const width = window.innerWidth;
    const height = window.innerHeight;
    const size = this.getScreenSize(width);

    return {
      size,
      width,
      height,
      isMobile: size === ScreenSize.XS || size === ScreenSize.SM,
      isTablet: size === ScreenSize.MD,
      isDesktop: size === ScreenSize.LG || size === ScreenSize.XL,
    };
  }

  private getDefaultScreenSizeInfo(): ScreenSizeInfo {
    return {
      size: ScreenSize.MD,
      width: 1024,
      height: 768,
      isMobile: false,
      isTablet: true,
      isDesktop: false,
    };
  }

  private getScreenSize(width: number): ScreenSize {
    if (width < breakpoints.sm) {
      return width < breakpoints.xs ? ScreenSize.XS : ScreenSize.SM;
    } else if (width < breakpoints.md) {
      return ScreenSize.MD;
    } else if (width < breakpoints.lg) {
      return ScreenSize.LG;
    } else {
      return ScreenSize.XL;
    }
  }
}
