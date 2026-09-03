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

// jsdom reports every width as 0, which makes app-overflow-labels hide every badge
export function stubOverflowLabelsLayout({
  containerWidth,
  badgeWidth = 100,
  counterWidth = 40,
}: {
  containerWidth: number;
  badgeWidth?: number;
  counterWidth?: number;
}): void {
  jest.spyOn(Element.prototype, 'getBoundingClientRect').mockImplementation(function (this: Element) {
    const width = this.matches('.overflow-labels')
      ? containerWidth
      : this.matches('[data-role="measure-counter"], [data-testid="overflow-counter"]')
        ? counterWidth
        : this.matches('[data-role="measure-badge"], [data-testid="visible-badge"]')
          ? badgeWidth
          : 0;
    return { width, height: 0, top: 0, left: 0, right: width, bottom: 0, x: 0, y: 0, toJSON: () => ({}) } as DOMRect;
  });
}
