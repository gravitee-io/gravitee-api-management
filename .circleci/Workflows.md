## Standard Pull Request Workflow

Here is the workflow for a standard pull request.

```mermaid
stateDiagram-v2
    [*] --> setup

    %% Backends
    setup --> validate
    validate --> build
    build --> test
    testPlugin: test - plugin
    testRepository: test - repository
    build --> testPlugin
    build --> testRepository

    sonarRestApi: Sonar - gravitee-apim-rest-api
    sonarGateway: Sonar - gravitee-apim-gateway
    sonarDefinition: Sonar - gravitee-apim-definition
    sonarPlugin: Sonar - gravitee-apim-plugin
    sonarRepository: Sonar - gravitee-apim-repository
    test --> sonarRestApi
    test --> sonarGateway
    test --> sonarDefinition
    testPlugin --> sonarPlugin
    testRepository --> sonarRepository

    %% Console
    webUILintConsole: Lint & test APIM Console
    setup --> webUILintConsole
    sonarConsole: Sonar - gravitee-apim-console-webui
    webUILintConsole --> sonarConsole

    storybook: Build Console Storybook
    chromatic: console-webui-chromatic-deployment
    setup --> storybook
    storybook --> chromatic

    webUIConsole: Build APIM Console
    setup --> webUIConsole
    deployAzure: console-webui-deploy-on-azure-storage
    webUIConsole --> deployAzure
    commentPR: console-webui-comment-pr-after-deployment
    deployAzure --> commentPR

    %% Portal
    webUILintPortal: Lint & test APIM Portal
    setup --> webUILintPortal
    sonarPortal: Sonar - gravitee-apim-portal-webui
    webUILintPortal --> sonarPortal

    webUIPortal: Build APIM Portal
    setup --> webUIPortal

    %% others
    e2eSDK: Generate e2e tests SDK
    build --> e2eSDK
    e2eLintBuild: Lint & build APIM e2e
    perfLintBuild: Lint & build APIM perf
    e2eSDK --> e2eLintBuild
    e2eSDK --> perfLintBuild
```

## Merge PR and run E2E PR Workflow

Here is the workflow triggered after a push on any branch containing `merge` or `run-e2e` in its name.
It's the same as the standard workflow, but with the addition of:

-   the community build
-   the E2E tests
-   the Cypress UI tests

```mermaid
stateDiagram-v2
    [*] --> setup
    communityBuild: community-build
    [*] --> communityBuild

    %% Backends
    setup --> validate
    validate --> build
    build --> test
    testPlugin: test - plugin
    testRepository: test - repository
    buildImages: build-images
    build --> testPlugin
    build --> testRepository
    build --> buildImages

    sonarRestApi: Sonar - gravitee-apim-rest-api
    sonarGateway: Sonar - gravitee-apim-gateway
    sonarDefinition: Sonar - gravitee-apim-definition
    sonarPlugin: Sonar - gravitee-apim-plugin
    sonarRepository: Sonar - gravitee-apim-repository
    test --> sonarRestApi
    test --> sonarGateway
    test --> sonarDefinition
    testPlugin --> sonarPlugin
    testRepository --> sonarRepository

    %% Console
    webUILintConsole: Lint & test APIM Console
    setup --> webUILintConsole
    sonarConsole: Sonar - gravitee-apim-console-webui
    webUILintConsole --> sonarConsole

    storybook: Build Console Storybook
    chromatic: console-webui-chromatic-deployment
    setup --> storybook
    storybook --> chromatic

    webUIConsole: Build APIM Console
    setup --> webUIConsole
    deployAzure: console-webui-deploy-on-azure-storage
    webUIConsole --> deployAzure
    commentPR: console-webui-comment-pr-after-deployment
    deployAzure --> commentPR

    %% Portal
    webUILintPortal: Lint & test APIM Portal
    setup --> webUILintPortal
    sonarPortal: Sonar - gravitee-apim-portal-webui
    webUILintPortal --> sonarPortal

    webUIPortal: Build APIM Portal
    setup --> webUIPortal

    %% others
    e2eSDK: Generate e2e tests SDK
    build --> e2eSDK
    e2eLintBuild: Lint & build APIM e2e
    perfLintBuild: Lint & build APIM perf
    e2eSDK --> e2eLintBuild
    e2eSDK --> perfLintBuild

    state join_state_e2e_test <<join>>
    buildImages --> join_state_e2e_test
    e2eLintBuild --> join_state_e2e_test
    e2eTestV3Mongo: Test APIM E2E - v3 - Mongo
    e2eTestV3JDBC: Test APIM E2E - v3 - JDBC
    e2eTestV3Bridge: Test APIM E2E - v3 - Bridge
    e2eTestJupiterMongo: Test APIM E2E - Jupiter - Mongo
    e2eTestJupiterJDBC: Test APIM E2E - Jupiter - JDBC
    e2eTestJupiterBridge: Test APIM E2E - Jupiter - Bridge
    join_state_e2e_test --> e2eTestV3Mongo
    join_state_e2e_test --> e2eTestV3JDBC
    join_state_e2e_test --> e2eTestV3Bridge
    join_state_e2e_test --> e2eTestJupiterMongo
    join_state_e2e_test --> e2eTestJupiterJDBC
    join_state_e2e_test --> e2eTestJupiterBridge

    state join_state_e2e_cypress <<join>>
    e2eLintBuild --> join_state_e2e_cypress
    webUIConsole --> join_state_e2e_cypress
    webUIPortal --> join_state_e2e_cypress
    buildImages --> join_state_e2e_cypress
    e2eCypress: Run Cypress UI tests
    join_state_e2e_cypress --> e2eCypress
```

## Merge on master and support branches Workflow

Here is the workflow triggered after a merge on the `master` or a support branch.
It's the same as the standard workflow, but with the addition of:

-   the community build
-   the E2E tests
-   the Cypress UI tests
-   the publish on Artifactory
-   the publish on Nexus
-   the deploy on Azure Dev Environment

```mermaid
stateDiagram-v2
    [*] --> setup
    communityBuild: community-build
    [*] --> communityBuild

    %% Backends
    setup --> validate
    validate --> build
    build --> test
    testPlugin: test - plugin
    testRepository: test - repository
    buildImages: build-images
    build --> testPlugin
    build --> testRepository
    build --> buildImages

    sonarRestApi: Sonar - gravitee-apim-rest-api
    sonarGateway: Sonar - gravitee-apim-gateway
    sonarDefinition: Sonar - gravitee-apim-definition
    sonarPlugin: Sonar - gravitee-apim-plugin
    sonarRepository: Sonar - gravitee-apim-repository
    test --> sonarRestApi
    test --> sonarGateway
    test --> sonarDefinition
    testPlugin --> sonarPlugin
    testRepository --> sonarRepository

    state join_state_publish <<join>>
    test --> join_state_publish
    testPlugin --> join_state_publish
    testRepository --> join_state_publish
    publishOnArtifactory: publish-on-artifactory
    publishOnNexus: publish-on-nexus
    join_state_publish --> publishOnArtifactory
    join_state_publish --> publishOnNexus

    %% Console
    webUILintConsole: Lint & test APIM Console
    setup --> webUILintConsole
    sonarConsole: Sonar - gravitee-apim-console-webui
    webUILintConsole --> sonarConsole

    storybook: Build Console Storybook
    chromatic: console-webui-chromatic-deployment
    setup --> storybook
    storybook --> chromatic

    webUIConsole: Build APIM Console
    setup --> webUIConsole
    deployAzure: console-webui-deploy-on-azure-storage
    webUIConsole --> deployAzure
    commentPR: console-webui-comment-pr-after-deployment
    deployAzure --> commentPR

    %% Portal
    webUILintPortal: Lint & test APIM Portal
    setup --> webUILintPortal
    sonarPortal: Sonar - gravitee-apim-portal-webui
    webUILintPortal --> sonarPortal

    webUIPortal: Build APIM Portal
    setup --> webUIPortal

    %% others
    e2eSDK: Generate e2e tests SDK
    build --> e2eSDK
    e2eLintBuild: Lint & build APIM e2e
    perfLintBuild: Lint & build APIM perf
    e2eSDK --> e2eLintBuild
    e2eSDK --> perfLintBuild

    state join_state_e2e_test <<join>>
    buildImages --> join_state_e2e_test
    e2eLintBuild --> join_state_e2e_test
    e2eTestV3Mongo: Test APIM E2E - v3 - Mongo
    e2eTestV3JDBC: Test APIM E2E - v3 - JDBC
    e2eTestV3Bridge: Test APIM E2E - v3 - Bridge
    e2eTestJupiterMongo: Test APIM E2E - Jupiter - Mongo
    e2eTestJupiterJDBC: Test APIM E2E - Jupiter - JDBC
    e2eTestJupiterBridge: Test APIM E2E - Jupiter - Bridge
    join_state_e2e_test --> e2eTestV3Mongo
    join_state_e2e_test --> e2eTestV3JDBC
    join_state_e2e_test --> e2eTestV3Bridge
    join_state_e2e_test --> e2eTestJupiterMongo
    join_state_e2e_test --> e2eTestJupiterJDBC
    join_state_e2e_test --> e2eTestJupiterBridge

    state join_state_e2e_cypress <<join>>
    e2eLintBuild --> join_state_e2e_cypress
    webUIConsole --> join_state_e2e_cypress
    webUIPortal --> join_state_e2e_cypress
    buildImages --> join_state_e2e_cypress
    e2eCypress: Run Cypress UI tests
    join_state_e2e_cypress --> e2eCypress

    state join_state_deploy <<join>>
    test --> join_state_deploy
    testPlugin --> join_state_deploy
    testRepository --> join_state_deploy
    buildImages --> join_state_deploy
    webUIConsole --> join_state_deploy
    webUIPortal --> join_state_deploy
    deployOnAzureCluster: deploy-on-azure-cluster
    join_state_deploy --> deployOnAzureCluster
```
