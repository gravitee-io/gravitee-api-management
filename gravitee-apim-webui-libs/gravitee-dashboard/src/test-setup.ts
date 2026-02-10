import { setupZoneTestEnv } from 'jest-preset-angular/setup-env/zone';
import 'chart.js/auto';
import 'jest-canvas-mock';

setupZoneTestEnv();

// Mock ResizeObserver to avoid errors in tests using canvas (Chartjs)
globalThis.ResizeObserver =
  globalThis.ResizeObserver ||
  jest.fn().mockImplementation(() => ({
    disconnect: jest.fn(),
    observe: jest.fn(),
    unobserve: jest.fn(),
  }));
