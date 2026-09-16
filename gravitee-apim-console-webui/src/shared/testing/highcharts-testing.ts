/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { EnvironmentProviders } from '@angular/core';
import { provideHighcharts } from 'highcharts-angular';
import * as Highcharts from 'highcharts';

/**
 * highcharts-angular 5 reads its Highcharts instance from an environment provider, declared once in
 * `AppModule`. Specs rendering a `<highcharts-chart>` need the same provider; handing over the
 * already imported instance also keeps Jest away from the ESM build the library loads by default.
 */
export const provideHighchartsTesting = (): EnvironmentProviders => provideHighcharts({ instance: () => Promise.resolve(Highcharts) });
