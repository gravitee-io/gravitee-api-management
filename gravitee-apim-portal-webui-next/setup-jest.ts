import '@angular/localize/init';
import 'chart.js/auto';
import 'jest-canvas-mock';
import { setupZoneTestEnv } from 'jest-preset-angular/setup-env/zone';

setupZoneTestEnv();

// mocking swagger-ui for tests as it contains some JS incompatible with Jest
// This means we will not be able to test Swagger in our components tests
jest.mock('swagger-ui', () => jest.fn(() => ({ initOAuth: () => jest.fn() })));

// Set the mock date globally for all tests
const MOCK_DATE = new Date(1466424490000); // UTC Time: Mon Jun 20 2016 12:08:10.000

// Need to define advanceTimers for async calls
jest.useFakeTimers({ advanceTimers: 1, now: MOCK_DATE }); // advance 1ms every 10ms

// Mock Date.now() so that it always returns the same date
Date.now = jest.fn(() => MOCK_DATE.getTime());

// Mock ResizeObserver to avoid errors in tests using canvas (Chartjs)
globalThis.ResizeObserver =
  globalThis.ResizeObserver ||
  jest.fn().mockImplementation(() => ({
    disconnect: jest.fn(),
    observe: jest.fn(),
    unobserve: jest.fn(),
  }));

window.HTMLElement.prototype.scrollIntoView = jest.fn();

// Hide the following error:
// "Could not parse CSS stylesheet"
const originalError = console.error;
beforeAll(() => {
  jest.spyOn(console, 'error').mockImplementation((...args: unknown[]) => {
    const firstArg = args[0];
    // jsdom CSS parsing
    if (firstArg instanceof Error && firstArg.message.includes('Could not parse CSS stylesheet')) {
      return;
    }
    // jsdom structured error
    if (typeof firstArg === 'object' && firstArg !== null && 'type' in firstArg && (firstArg as any).type === 'css parsing') {
      return;
    }
    originalError.call(console, ...args);
  });
});
