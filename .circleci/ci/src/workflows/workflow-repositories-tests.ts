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
import { Config, workflow, Workflow } from '@circleci/circleci-config-sdk';
import { BuildBackendJob, SetupJob } from '../jobs';
import { ElasticTestContainerJob, JdbcTestContainerJob, MongoTestContainerJob, RedisTestContainerJob } from '../jobs/test-container';

import { CircleCIEnvironment } from '../pipelines';

export class RepositoriesTestsWorkflow {
  static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const setupJob = SetupJob.create(dynamicConfig);
    const buildJob = BuildBackendJob.create(dynamicConfig, environment);
    const jdbcTestContainerJob = JdbcTestContainerJob.create(dynamicConfig, environment);
    const mongoTestContainerJob = MongoTestContainerJob.create(dynamicConfig, environment);
    const elasticTestContainerJob = ElasticTestContainerJob.create(dynamicConfig, environment);
    const opensearchTestContainerJob = ElasticTestContainerJob.create(dynamicConfig, environment);
    const redisTestContainerJob = RedisTestContainerJob.create(dynamicConfig, environment);

    dynamicConfig.addJob(setupJob);
    dynamicConfig.addJob(buildJob);
    dynamicConfig.addJob(jdbcTestContainerJob);
    dynamicConfig.addJob(mongoTestContainerJob);
    dynamicConfig.addJob(elasticTestContainerJob);
    dynamicConfig.addJob(opensearchTestContainerJob);
    dynamicConfig.addJob(redisTestContainerJob);

    const setupJobName = 'Setup';
    const buildJobName = 'Build';

    return new Workflow('repositories-tests', [
      new workflow.WorkflowJob(setupJob, { name: setupJobName, context: ['cicd-orchestrator'] }),
      new workflow.WorkflowJob(buildJob, { name: buildJobName, context: ['cicd-orchestrator'], requires: [setupJobName] }),
      new workflow.WorkflowJob(jdbcTestContainerJob, {
        name: 'Management repository tests - JDBC - << matrix.jdbcType >>',
        context: ['cicd-orchestrator'],
        requires: [buildJobName],
        matrix: {
          jdbcType: [
            'postgresql~14',
            'postgresql~15',
            'postgresql~16',
            'postgresql~17',
            'postgresql~18',
            'mariadb~10.6',
            'mariadb~10.11',
            'mariadb~11.4',
            'mariadb~11.8',
            'mariadb~12.1',
            'mysql~8.0',
            'mysql~8.4',
            'sqlserver~2017-latest',
            'sqlserver~2019-latest',
            'sqlserver~2022-latest',
            'sqlserver~2025-latest',
          ],
        },
      }),
      // Reproduces MySQL HeatWave / Group Replication strict-PK behaviour so the changelog
      // migrations are exercised against sql_require_primary_key=ON. Scoped to MySQL only —
      // the MariaDB 12.1 testcontainer refused to start with --sql-require-primary-key=ON
      // (container exits 1 before any test runs), and MariaDB strict-PK is not the customer
      // scenario this PR was filed for.
      // Single-cell matrix is intentional. The cleaner `parameters: { … }` form
      // CircleCI's docs suggest is rejected at runtime here with
      // "Unexpected argument(s): parameters" — the @circleci/circleci-config-sdk
      // emits the keys nested under a `parameters:` map, but the runtime expects
      // them at workflow-job level. Matrix is the only form the SDK serialises in
      // the shape CircleCI accepts.
      new workflow.WorkflowJob(jdbcTestContainerJob, {
        name: 'Management repository tests - JDBC - << matrix.jdbcType >> (strict-PK)',
        context: ['cicd-orchestrator'],
        requires: [buildJobName],
        matrix: {
          jdbcType: ['mysql~8.4'],
          strictPrimaryKey: ['true'],
        },
      }),
      new workflow.WorkflowJob(mongoTestContainerJob, {
        name: 'Management repository tests - Mongo << matrix.mongoVersion >>',
        context: ['cicd-orchestrator'],
        requires: [buildJobName],
        matrix: {
          mongoVersion: ['4.4', '5.0', '6.0', '7.0', '8.0'],
        },
      }),
      new workflow.WorkflowJob(elasticTestContainerJob, {
        name: 'Analytics repository tests - ElasticSearch << matrix.engineVersion >>',
        context: ['cicd-orchestrator'],
        requires: [buildJobName],
        matrix: {
          engineType: ['elasticsearch'],
          engineVersion: ['7.17.29', '8.19.12', '9.3.1'],
        },
      }),
      new workflow.WorkflowJob(opensearchTestContainerJob, {
        name: 'Analytics repository tests - OpenSearch << matrix.engineVersion >>',
        context: ['cicd-orchestrator'],
        requires: [buildJobName],
        matrix: {
          engineType: ['opensearch'],
          engineVersion: ['1', '2', '3'],
        },
      }),
      new workflow.WorkflowJob(redisTestContainerJob, {
        name: 'Rate Limit repository tests - Redis << matrix.redisImage >>',
        context: ['cicd-orchestrator'],
        requires: [buildJobName],
        matrix: {
          redisImage: [
            'redis/redis-stack:6.2.6-v20',
            'redis/redis-stack:7.0.6-RC9',
            'redis/redis-stack:7.2.0-v20',
            'redis/redis-stack:7.4.0-v8',
            'redis:8',
          ],
        },
      }),
    ]);
  }
}
