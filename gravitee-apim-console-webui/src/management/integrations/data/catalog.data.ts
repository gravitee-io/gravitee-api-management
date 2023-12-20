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

interface CatalogData {
  iconPath: string;
  title: string;
  categories: string[];
  description: string;
  status: string;
  isAdd: boolean;
}

export const catalogData: CatalogData[] = [
  {
    iconPath: "assets/logo_aws.svg",
    title: "AWS Gateway",
    categories: ["Mediate", "Federate"],
    description: "Connect to Amazon API Gateway for API management and data integration. Customize API proxies with Gravitee Gateway or expose data sources directly.",
    status: "No integration configured",
    isAdd: true
  },
  {
    iconPath: "assets/logo_solace.svg",
    title: "Solace",
    categories: ["Mediate", "Federate"],
    description: "Integrate Solace for streamlined API management. Choose Gravitee Gateway or direct data source exposure to suit your use case.",
    status: "No integration configured",
    isAdd: true
  },
  {
    iconPath: "assets/logo_confluent.svg",
    title: "Confluent",
    categories: ["Mediate", "Federate"],
    description: "Discover AsyncAPIs in Kafka and expose them with Gravitee. Use protocol translation or use Gravitee`s Consumer Portal for direct client connection.",
    status: "No integration configured",
    isAdd: false
  },
  {
    iconPath: "assets/logo_snowflake.svg",
    title: "Snowflake",
    categories: ["Mediate"],
    description: "Discover database schemas, expose as API products with access control, traffic shaping, and analytics for seamless data access.",
    status: "No integration configured",
    isAdd: false
  },
  {
    iconPath: "assets/logo_salesforce.svg",
    title: "Salesforce",
    categories: ["Mediate"],
    description: "Expose Salesforce objects as API products, enhancing functionality and extending reach. Enable consumption synchronously or asynchronously with REST or CDC",
    status: "No integration configured",
    isAdd: false
  },
  {
    iconPath: "assets/logo_apigee.svg",
    title: "Apigee",
    categories: ["Mediate", "Federate"],
    description: "Import Apigee Proxy or Product APIs to Gravitee. Manage via Gravitee Gateway or the Consumer Portal for discovery and subscriptions.",
    status: "No integration configured",
    isAdd: false
  },
];
