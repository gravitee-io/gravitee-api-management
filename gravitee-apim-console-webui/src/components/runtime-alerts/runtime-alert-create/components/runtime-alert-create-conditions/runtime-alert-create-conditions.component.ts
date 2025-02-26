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
import { ChangeDetectorRef, Component, Input, OnChanges, OnInit, SimpleChanges } from "@angular/core";
import {
  ControlContainer,
  FormBuilder, FormControl,
  FormGroup, FormGroupDirective,
  Validators
} from "@angular/forms";

import {
  AggregationCondition,
  Conditions,
  ConditionType,
  Metrics,
  Operator,
  Scope
} from "../../../../../entities/alert";
import { Rule } from "../../../../../entities/alerts/rule.metrics";
import { AlertTriggerEntity } from "../../../../../entities/alerts/alertTriggerEntity";
import {
  CompareCondition, StringCompareCondition, StringCondition,
  ThresholdCondition,
  ThresholdRangeCondition
} from "../../../../../entities/alerts/conditions";
import { ApiMetrics } from "../../../../../entities/alerts/api.metrics";
import { Api } from "@gravitee/ui-components/src/molecules/gv-card-full/gv-card-full.stories";

@Component({
  standalone: false,
  selector: "runtime-alert-create-conditions",
  templateUrl: "./runtime-alert-create-conditions.component.html",
  styleUrls: ["./runtime-alert-create-conditions.component.scss"],
  viewProviders: [{ provide: ControlContainer, useExisting: FormGroupDirective }]
})
export class RuntimeAlertCreateConditionsComponent implements OnInit, OnChanges {
  // @Input({ required: true }) set rule(value: Rule) {
  //   if (value) {
  //     this.ruleType = `${value.source}@${value.type}`;
  //     this.conditionsForm = RuntimeAlertCreateConditionsFactory.create(this.ruleType);
  //     this.metrics = Metrics.filterByScope(Rule.findByScopeAndType(this.referenceType, this.ruleType)?.metrics ?? [], this.referenceType);
  //   }
  // }
  @Input() public alertToUpdate: AlertTriggerEntity;
  @Input() public referenceType: Scope;
  @Input() public referenceId: string;
  @Input() public rule: Rule;

  public ruleType: string;
  public metrics: Metrics[];
  public types: string[];
  public parentForm: FormGroup;
  public conditionsForm: FormGroup;


  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly formGroupDirective: FormGroupDirective,
    private readonly changeDetectorRef: ChangeDetectorRef,
  ) {

  }

  ngOnInit() {
    this.parentForm = this.formGroupDirective.form;
    this.conditionsForm = this.parentForm?.get("conditionsForm") as FormGroup;
  }

  ngOnChanges(changes: SimpleChanges) {
    this.rule = changes?.rule?.currentValue;
    if (this.rule) {
      console.log('this.alertToUpdate: ', this.alertToUpdate);
      this.ruleType = `${this.rule.source}@${this.rule.type}`;
      this.metrics = Metrics.filterByScope(Rule.findByScopeAndType(this.referenceType, this.ruleType)?.metrics ?? [], this.referenceType);
      this.createForm(this.ruleType);
    }
  }

  createForm(rule: string) {
    this.resetForm();

    if (rule.endsWith("@MISSING_DATA")) {
      console.log('1. endsWith("@MISSING_DATA")')
      this.conditionsForm.addControl("duration", new FormControl<number>(null, [Validators.required, Validators.min(1)]));
      this.conditionsForm.addControl("timeUnit", new FormControl<string>(null, [Validators.required]));
      this.conditionsForm.addControl("type", new FormControl<string>("MISSING_DATA", [Validators.required]));
      return;
    }

    switch (rule) {
      case "REQUEST@METRICS_SIMPLE_CONDITION":
        console.log('2: REQUEST@METRICS_SIMPLE_CONDITION');

        if(this.alertToUpdate) {
          this.addControlsWithData();
        } else {
          this.conditionsForm.addControl("metric", new FormControl<Metrics>(null, [Validators.required]));
          this.conditionsForm.addControl("type", new FormControl<string>({
            value: null,
            disabled: true
          }, [Validators.required]));
        }
        break;

      case "REQUEST@METRICS_AGGREGATION":
        console.log('3: REQUEST@METRICS_AGGREGATION');

        this.conditionsForm.addControl("metric", new FormControl<Metrics>(null, [Validators.required]));
        this.conditionsForm.addControl("type", new FormControl<string>("AGGREGATION", [Validators.required]));
        this.conditionsForm.addControl("function", new FormControl<string>(null, [Validators.required]));
        this.conditionsForm.addControl("operator", new FormControl<string>(null, [Validators.required]));
        this.conditionsForm.addControl("threshold", new FormControl<number>(null, [Validators.required]));
        this.conditionsForm.addControl("duration", new FormControl<number>(null, [Validators.required, Validators.min(1)]));
        this.conditionsForm.addControl("timeUnit", new FormControl<string>(null, [Validators.required]));
        this.conditionsForm.addControl("projections", new FormGroup({ property: new FormControl<string>(null) }));
        break;


      case "REQUEST@METRICS_RATE":
        console.log('4. REQUEST@METRICS_RATE');
        this.conditionsForm.addControl("comparison", new FormGroup({
          metric: new FormControl<Metrics>(null, [Validators.required]),
          type: new FormControl<string>({ value: null, disabled: true }, [Validators.required])
        }));
        this.conditionsForm.addControl("type", new FormControl<string>("RATE", [Validators.required]));
        this.conditionsForm.addControl("operator", new FormControl<string>(null, [Validators.required]));
        this.conditionsForm.addControl("threshold", new FormControl<number>(null, [Validators.required]));
        this.conditionsForm.addControl("duration", new FormControl<number>(null, [Validators.required, Validators.min(1), Validators.max(100)]));
        this.conditionsForm.addControl("timeUnit", new FormControl<string>(null, [Validators.required]));
        this.conditionsForm.addControl("projections", new FormGroup({
          property: new FormControl<string>(null)
        }));
        break;

      case "ENDPOINT_HEALTH_CHECK@API_HC_ENDPOINT_STATUS_CHANGED":
        console.log('5. ENDPOINT_HEALTH_CHECK@API_HC_ENDPOINT_STATUS_CHANGED');
        this.conditionsForm.addControl("projections", new FormGroup({
          property: new FormControl<string>(null)
        }));
        this.conditionsForm.addControl('type', new FormControl<string>("API_HC_ENDPOINT_STATUS_CHANGED", [Validators.required]));
        break;
    }
  }

  public resetForm() {
    Object.keys(this.conditionsForm.getRawValue()).forEach((key) => {
      this.conditionsForm.removeControl(key);
    });
  }

  public addControlsWithData () {
    const thresholdCondition = this.alertToUpdate.conditions[0] as ThresholdCondition | ThresholdRangeCondition | CompareCondition | StringCondition;

    // ignoreCase :  true
    // operator : "EQUALS"
    // pattern: "API_KEY_INVALID"
    // property : "error.key"
    // type : "STRING"

    console.log('alertToUpdate', this.alertToUpdate)


    const thresholdConditionType = thresholdCondition.type;
    // const metric = this.metrics.find((metric ) => metric.key === thresholdCondition.property);

    const metric = ApiMetrics.METRICS.find((metric) => metric.key === thresholdCondition.property)


    console.log('ApiMetrics.METRICS', ApiMetrics.METRICS);
    console.log('metric', metric);

    // this.conditionsForm.addControl("metric", new FormControl<Metrics>(metric, [Validators.required]));
    // this.conditionsForm.addControl("type", new FormControl<string>(thresholdConditionType, [Validators.required]));

    this.conditionsForm.addControl("metric", new FormControl<ApiMetrics>(metric, [Validators.required]));
    this.conditionsForm.addControl("type", new FormControl<string>(null, [Validators.required])); // poswinno być STRING " ale nie wchodzi!!

    if (ConditionType.THRESHOLD === thresholdConditionType) {
      const operators: Operator[] = AggregationCondition.OPERATORS;
      const operator = operators.find((operator) => operator.key === thresholdCondition.operator);

      this.conditionsForm.addControl('operator', new FormControl(operator, Validators.required));
      this.conditionsForm.addControl('threshold', new FormControl(thresholdCondition.threshold, [Validators.required, Validators.min(1)]));
    }

    else if (ConditionType.THRESHOLD_RANGE === thresholdConditionType) {
      this.conditionsForm.addControl('lowThreshold', new FormControl(thresholdCondition.thresholdLow, Validators.required));
      this.conditionsForm.addControl('highThreshold', new FormControl(thresholdCondition.thresholdHigh, [Validators.required]));
    }

    else if (ConditionType.COMPARE === thresholdConditionType) {
      const condition = Conditions.findByType(thresholdConditionType)
      const operators = condition.getOperators();
      const operator = operators.find((operator) => operator.key === thresholdCondition.operator);

      const thresholdConditionProperty = this.metrics.find((metric ) => metric.key === thresholdCondition.property2);

      this.conditionsForm.addControl('operator', new FormControl(operator, Validators.required));
      this.conditionsForm.addControl('multiplier', new FormControl(thresholdCondition.multiplier, [Validators.required, Validators.min(1)]));
      this.conditionsForm.addControl('property', new FormControl(thresholdConditionProperty, [Validators.required]));
    }

    else if (ConditionType.STRING === thresholdConditionType) {
      const condition = Conditions.findByType(thresholdConditionType)
      const operators = condition.getOperators();
      const operator = operators.find((operator) => operator.key === thresholdCondition.operator);
      // key :    "EQUALS"
      // name :  "equals to"

      this.conditionsForm.addControl('operator', new FormControl(operator, Validators.required));
      this.conditionsForm.addControl('pattern', new FormControl(null, Validators.required));
    }
  }


}
