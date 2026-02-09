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

import { Converter } from '../../../converter';
import { TimeSeries, TimeSeriesBucket, TimeSeriesResponse } from '../../../widget/model/response/time-series-response';

@Injectable({
  providedIn: 'root',
})
export class LineConverterService implements Converter {
  public convert(data: TimeSeriesResponse): ChartData<'line', number[], string> {
    const labels: string[] = [];
    const datasets: ChartData<'line', number[], string>['datasets'] = [];

    if (!data?.metrics?.length) {
      return { labels, datasets };
    }

    this.extractTimeLabelsFromFirstMetric(data, labels);
    this.processAllMetrics(data, datasets);

    return { labels, datasets };
  }

  private extractTimeLabelsFromFirstMetric(data: TimeSeriesResponse, labels: string[]): void {
    // Assuming all metrics share the same time buckets (same timestamps & order),
    // build labels from the first metric only.
    const firstMetric = data.metrics?.[0];

    if (firstMetric?.buckets?.length) {
      firstMetric.buckets.forEach(timeBucket => {
        labels.push(this.toTimeLabel(timeBucket));
      });
    }
  }

  private processAllMetrics(data: TimeSeriesResponse, datasets: ChartData<'line', number[], string>['datasets']): void {
    data.metrics?.forEach((metric, metricIndex) => {
      this.processMetric(metric, metricIndex, datasets);
    });
  }

  private processMetric(metric: TimeSeries, metricIndex: number, datasets: ChartData<'line', number[], string>['datasets']): void {
    const metricBuckets = metric.buckets ?? [];
    const bucketCount = metricBuckets.length;

    if (!bucketCount) {
      return;
    }

    const hasNestedBuckets = this.hasNestedBuckets(metricBuckets);
    const baseMetricLabel = this.getBaseMetricLabel(metric, metricIndex);

    if (hasNestedBuckets) {
      this.buildGroupedDatasetsFromNestedBuckets(metricBuckets, bucketCount, datasets);
    } else {
      this.buildSimpleDatasetFromMetric(metricBuckets, baseMetricLabel, datasets);
    }
  }

  private hasNestedBuckets(metricBuckets: TimeSeriesBucket[]): boolean {
    return (metricBuckets[0].buckets?.length ?? 0) > 0;
  }

  private getBaseMetricLabel(metric: TimeSeries, metricIndex: number): string {
    return metric.name || `Metric ${metricIndex + 1}`;
  }

  private buildGroupedDatasetsFromNestedBuckets(
    metricBuckets: TimeSeriesBucket[],
    bucketCount: number,
    datasets: ChartData<'line', number[], string>['datasets'],
  ): void {
    const groupMap = this.aggregateNestedBucketValuesByGroup(metricBuckets, bucketCount);

    groupMap.forEach((values, groupName) => {
      datasets.push({
        label: `${groupName}`,
        data: values,
      });
    });
  }

  private aggregateNestedBucketValuesByGroup(metricBuckets: TimeSeriesBucket[], bucketCount: number): Map<string, number[]> {
    const groupMap = new Map<string, number[]>();

    metricBuckets.forEach((timeBucket, timeIndex) => {
      timeBucket.buckets?.forEach(nestedBucket => {
        const groupName = nestedBucket.name || nestedBucket.key;
        if (!groupName) {
          return;
        }

        if (!groupMap.has(groupName)) {
          groupMap.set(groupName, new Array(bucketCount).fill(0));
        }

        const values = groupMap.get(groupName)!;

        values[timeIndex] = nestedBucket.measures?.[0]?.value ?? 0;
      });
    });

    return groupMap;
  }

  private buildSimpleDatasetFromMetric(
    metricBuckets: TimeSeriesBucket[],
    baseMetricLabel: string,
    datasets: ChartData<'line', number[], string>['datasets'],
  ): void {
    const dataValues: number[] = metricBuckets.map(bucket => this.getBucketValue(bucket));

    datasets.push({
      label: baseMetricLabel,
      data: dataValues,
    });
  }

  private getBucketValue(bucket: TimeSeriesBucket): number {
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

    // Fallback to key when timestamp is not available or cannot be parsed as a valid date.
    return bucket.key ?? '';
  }
}
