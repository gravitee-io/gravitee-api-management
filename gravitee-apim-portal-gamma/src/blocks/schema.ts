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
  },
});
