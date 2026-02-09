# @gravitee/gravitee-dashboard

## Overview

Gravitee-Dashboard is a library that provides a set of Angular components to build dashboards.

Dashboards are composed of several components:

- Grid: a layout system
- Widget : a reusable component to display data
- Chart: a set of charts components

## Dependencies

Gravitee-Dashboard is based on top of two libraries.

- [Gridster2](https://github.com/tiberiuzuld/angular-gridster2): provides a layout system with drag and drop support.
- [Highcharts](https://api.highcharts.com/highcharts): provides a set of charts components.

### Peer Dependencies

- @angular/common: 19.2.0
- @angular/core: 19.2.0
- tslib: 2.3.1
- angular-gridster2: 20.2.2
- highcharts: 9.3.3
- highcharts-angular: 3.1.2

## Project Structure

```
src/
├── lib/                    # Library source code
│   ├── components/         # Reusable components
│   ├── directives/         # Custom directives
│   ├── pipes/             # Custom pipes
│   ├── services/          # Shared services
│   └── types/             # TypeScript type definitions
├── public-api.ts          # Public API exports
└── index.ts               # Library entry point
```

## Theming

The library supports theming through CSS variables. You can customize the appearance of the components by overriding the default CSS variables in your application's global styles.
