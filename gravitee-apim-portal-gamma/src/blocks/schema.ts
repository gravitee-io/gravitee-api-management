import { BlockNoteSchema, defaultBlockSpecs } from '@blocknote/core';
import { withMultiColumn } from '@blocknote/xl-multi-column';
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

export const schema = withMultiColumn(
  BlockNoteSchema.create({
    blockSpecs: {
      ...defaultBlockSpecs,
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
  }),
);
