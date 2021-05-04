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
class CircularPercentageController {
  private score = 0;
  private percentage = 0;
  private percentageCircle = 100;
  private qualityMetricCssClass: string;

  getScore() {
    if (this.score > 0) {
      this.percentage = parseInt((this.score * 100).toFixed(0), 10);
      this.percentageCircle = this.percentage;
    }

    if (this.percentage < 50) {
      this.qualityMetricCssClass = 'gravitee-qm-score-bad';
    } else if (this.percentage >= 50 && this.percentage < 80) {
      this.qualityMetricCssClass = 'gravitee-qm-score-medium';
    } else {
      this.qualityMetricCssClass = 'gravitee-qm-score-good';
    }
    return this.percentage;
  }
}

export default CircularPercentageController;
