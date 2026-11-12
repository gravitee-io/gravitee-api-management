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
import { Injectable } from '@angular/core';
import { ChartData } from 'chart.js';

import { ChartValues, Converter } from '../../../converter';
import { MeasureName } from '../../../widget/model/request/enum/measure-name';
import { TimeSeries, TimeSeriesBucket, TimeSeriesResponse } from '../../../widget/model/response/time-series-response';

/** The measures that read 0, not no value, over no request, as the analytics engine serves them (`MetricSpec.Measure#emptyValue`). */
const COUNTED_MEASURES: MeasureName[] = ['COUNT', 'SUM', 'PERCENTAGE'];

@Injectable({
  providedIn: 'root',
})
export class TimeSeriesConverterService implements Converter {
  public convert(data: TimeSeriesResponse): ChartData<'line' | 'bar', ChartValues, string> {
    if (!data?.metrics?.length) {
      return { labels: [], datasets: [] };
    }

    const labels = this.extractTimeLabels(data);
    const datasets = this.processAllMetrics(data);

    return { labels, datasets };
  }

  private extractTimeLabels(data: TimeSeriesResponse): string[] {
    const labels: string[] = [];
    const bucketsForLabels = data.buckets?.length ? data.buckets : data.metrics?.[0]?.buckets;

    if (bucketsForLabels) {
      bucketsForLabels.forEach(timeBucket => {
        labels.push(this.toTimeLabel(timeBucket));
      });
    }
    return labels;
  }

  private processAllMetrics(data: TimeSeriesResponse): ChartData<'line' | 'bar', ChartValues, string>['datasets'] {
    return (data.metrics ?? []).flatMap((metric, metricIndex) => this.processMetric(metric, metricIndex));
  }

  private processMetric(metric: TimeSeries, metricIndex: number): ChartData<'line' | 'bar', ChartValues, string>['datasets'] {
    const metricBuckets = metric.buckets ?? [];
    const bucketCount = metricBuckets.length;

    if (!bucketCount) {
      return [];
    }

    const hasNestedBuckets = this.hasNestedBuckets(metricBuckets);
    const baseMetricLabel = this.getBaseMetricLabel(metric, metricIndex);

    if (hasNestedBuckets) {
      return this.buildGroupedDatasetsFromNestedBuckets(metricBuckets, bucketCount);
    } else {
      return this.buildSimpleDatasetFromMetric(metricBuckets, baseMetricLabel);
    }
  }

  private hasNestedBuckets(metricBuckets: TimeSeriesBucket[]): boolean {
    return metricBuckets.some(bucket => (bucket.buckets?.length ?? 0) > 0);
  }

  private getBaseMetricLabel(metric: TimeSeries, metricIndex: number): string {
    return metric.name || `Metric ${metricIndex + 1}`;
  }

  private buildGroupedDatasetsFromNestedBuckets(
    metricBuckets: TimeSeriesBucket[],
    bucketCount: number,
  ): ChartData<'line' | 'bar', ChartValues, string>['datasets'] {
    const datasets: ChartData<'line' | 'bar', ChartValues, string>['datasets'] = [];
    const groupMap = this.aggregateNestedBucketValuesByGroup(metricBuckets, bucketCount);

    groupMap.forEach((values, groupName) => {
      datasets.push({
        label: `${groupName}`,
        data: values,
      });
    });
    return datasets;
  }

  private aggregateNestedBucketValuesByGroup(metricBuckets: TimeSeriesBucket[], bucketCount: number): Map<string, ChartValues> {
    const groupMap = new Map<string, ChartValues>();

    metricBuckets.forEach((timeBucket, timeIndex) => {
      timeBucket.buckets?.forEach(nestedBucket => {
        const groupName = nestedBucket.name || nestedBucket.key;
        if (!groupName) {
          return;
        }

        const measure = nestedBucket.measures?.[0];
        if (!groupMap.has(groupName)) {
          groupMap.set(groupName, new Array(bucketCount).fill(this.valueOfAnAbsentGroup(measure?.name)));
        }

        const values = groupMap.get(groupName)!;

        values[timeIndex] = measure?.value ?? null;
      });
    });

    return groupMap;
  }

  /** A group missing from a time bucket counted nothing there, but has no average, bound or percentile. */
  private valueOfAnAbsentGroup(measureName: MeasureName | undefined): number | null {
    return measureName && COUNTED_MEASURES.includes(measureName) ? 0 : null;
  }

  private buildSimpleDatasetFromMetric(
    metricBuckets: TimeSeriesBucket[],
    baseMetricLabel: string,
  ): ChartData<'line' | 'bar', ChartValues, string>['datasets'] {
    const dataValues: ChartValues = metricBuckets.map(bucket => this.getBucketValue(bucket));

    return [
      {
        label: baseMetricLabel,
        data: dataValues,
      },
    ];
  }

  private getBucketValue(bucket: TimeSeriesBucket): number | null {
    if (bucket.measures?.length) {
      return bucket.measures[0].value;
    }

    if (bucket.buckets?.length) {
      return bucket.buckets.reduce((acc, nested) => acc + (nested.measures?.[0]?.value ?? 0), 0);
    }

    return 0;
  }

  private toTimeLabel(bucket: TimeSeriesBucket): string {
    if (bucket.timestamp != null) {
      const date = new Date(bucket.timestamp);
      if (!Number.isNaN(date.getTime())) {
        return date.toISOString();
      }
    }

    return bucket.key ?? '';
  }
}
