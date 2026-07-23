import { BlockNoteSchema, defaultBlockSpecs } from '@blocknote/core';
import { ApiCatalogBlock } from './ApiCatalogBlock/ApiCatalogBlock';
import { ApiMetadataBlock } from './ApiMetadataBlock/ApiMetadataBlock';
import { BannerBlock } from './BannerBlock/BannerBlock';
import { CardBlock } from './CardBlock/CardBlock';
import { ButtonBlock } from './ButtonBlock/ButtonBlock';
import { HtmlBlock } from './HtmlBlock/HtmlBlock';
import { MarkdownBlock } from './MarkdownBlock/MarkdownBlock';
import { SectionBlock } from './SectionBlock/SectionBlock';
import { ContainerBlock } from './ContainerBlock/ContainerBlock';
import { SubscriptionFlowBlock } from './SubscriptionFlowBlock/SubscriptionFlowBlock';
import { SubscriptionViewerBlock } from './SubscriptionViewerBlock/SubscriptionViewerBlock';
import { ApplicationsBlock } from './ApplicationsBlock/ApplicationsBlock';
import { InstallMcpBlock } from './InstallMcpBlock/InstallMcpBlock';
import { ApiOperationsBlock } from './ApiOperationsBlock/ApiOperationsBlock';
import { ApiSchemasBlock } from './ApiSchemasBlock/ApiSchemasBlock';
import { ApiTryItBlock } from './ApiTryItBlock/ApiTryItBlock';
import { ApiCodeSamplesBlock } from './ApiCodeSamplesBlock/ApiCodeSamplesBlock';
import { AiWorkspaceOverviewBlock } from './AiWorkspaceBlocks/AiWorkspaceOverviewBlock';
import { AiKeyBlock } from './AiWorkspaceBlocks/AiKeyBlock';
import { AiEndpointBlock } from './AiWorkspaceBlocks/AiEndpointBlock';
import { AiBudgetBlock } from './AiWorkspaceBlocks/AiBudgetBlock';
import { AiModelsBlock } from './AiWorkspaceBlocks/AiModelsBlock';
import { AiSnippetsBlock } from './AiWorkspaceBlocks/AiSnippetsBlock';
import { AiUsageHistoryBlock } from './AiWorkspaceBlocks/AiUsageHistoryBlock';
import { AiDashboardBlock } from './AiWorkspaceBlocks/AiDashboardBlock';
import { AiCredentialsBlock } from './AiWorkspaceBlocks/AiCredentialsBlock';
import { AiAnalyticsBlock } from './AiWorkspaceBlocks/AiAnalyticsBlock';
import { AiTryItBlock } from './AiWorkspaceBlocks/AiTryItBlock';
import { ColumnBlock, ColumnListBlock } from './MultiColumnBlock/multi-column-blocks';
import './MultiColumnBlock/multi-column.module.scss';

export const schema = BlockNoteSchema.create({
  blockSpecs: {
    ...defaultBlockSpecs,
    columnList: ColumnListBlock,
    column: ColumnBlock,
    graviteeApiCatalog: ApiCatalogBlock(),
    graviteeApiMetadata: ApiMetadataBlock(),
    graviteeBanner: BannerBlock(),
    graviteeCard: CardBlock(),
    graviteeButton: ButtonBlock(),
    graviteeHtml: HtmlBlock(),
    graviteeMarkdown: MarkdownBlock(),
    graviteeSection: SectionBlock(),
    graviteeContainer: ContainerBlock,
    graviteeSubscriptionFlow: SubscriptionFlowBlock(),
    graviteeSubscriptionViewer: SubscriptionViewerBlock(),
    graviteeApplications: ApplicationsBlock(),
    graviteeInstallMcp: InstallMcpBlock(),
    graviteeApiOperations: ApiOperationsBlock(),
    graviteeApiSchemas: ApiSchemasBlock(),
    graviteeApiTryIt: ApiTryItBlock(),
    graviteeApiCodeSamples: ApiCodeSamplesBlock(),
    graviteeAiWorkspaceOverview: AiWorkspaceOverviewBlock(),
    graviteeAiKey: AiKeyBlock(),
    graviteeAiEndpoint: AiEndpointBlock(),
    graviteeAiBudget: AiBudgetBlock(),
    graviteeAiModels: AiModelsBlock(),
    graviteeAiSnippets: AiSnippetsBlock(),
    graviteeAiUsageHistory: AiUsageHistoryBlock(),
    graviteeAiDashboard: AiDashboardBlock(),
    graviteeAiCredentials: AiCredentialsBlock(),
    graviteeAiAnalytics: AiAnalyticsBlock(),
    graviteeAiTryIt: AiTryItBlock(),
  },
});
