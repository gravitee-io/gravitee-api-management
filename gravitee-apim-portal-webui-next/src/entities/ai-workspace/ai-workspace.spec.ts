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
import { formatAiWorkspaceBudget, formatAiWorkspaceCost } from './ai-workspace';

describe('formatAiWorkspaceBudget', () => {
  it('returns a dash when the budget has no amount', () => {
    expect(formatAiWorkspaceBudget()).toBe('—');
    expect(formatAiWorkspaceBudget(null)).toBe('—');
    expect(formatAiWorkspaceBudget({})).toBe('—');
  });

  it('returns the amount when the period is missing', () => {
    expect(formatAiWorkspaceBudget({ amount: 50.05 })).toBe('$50.05');
  });

  it('returns the amount and period', () => {
    expect(formatAiWorkspaceBudget({ amount: 50.05, period: 'WEEK' })).toBe('$50.05 / week');
  });
});

describe('formatAiWorkspaceCost', () => {
  it('returns zero when the cost is missing', () => {
    expect(formatAiWorkspaceCost()).toBe('$0.00');
    expect(formatAiWorkspaceCost(null)).toBe('$0.00');
  });

  it('formats a numeric cost', () => {
    expect(formatAiWorkspaceCost(1.2)).toBe('$1.20');
  });
});
