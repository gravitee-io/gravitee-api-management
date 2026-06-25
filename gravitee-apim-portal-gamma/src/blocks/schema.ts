import { BlockNoteSchema, defaultBlockSpecs } from '@blocknote/core';
import { withMultiColumn } from '@blocknote/xl-multi-column';
import { ApiListBlock } from './ApiListBlock/ApiListBlock';
import { BannerBlock } from './BannerBlock/BannerBlock';
import { CardBlock } from './CardBlock/CardBlock';
import { ButtonBlock } from './ButtonBlock/ButtonBlock';
import { HtmlBlock } from './HtmlBlock/HtmlBlock';
import { MarkdownBlock } from './MarkdownBlock/MarkdownBlock';
import { SectionBlock } from './SectionBlock/SectionBlock';
import { ContainerBlock } from './ContainerBlock/ContainerBlock';

export const schema = withMultiColumn(
  BlockNoteSchema.create({
    blockSpecs: {
      ...defaultBlockSpecs,
      graviteeApiList: ApiListBlock(),
      graviteeBanner: BannerBlock(),
      graviteeCard: CardBlock(),
      graviteeButton: ButtonBlock(),
      graviteeHtml: HtmlBlock(),
      graviteeMarkdown: MarkdownBlock(),
      graviteeSection: SectionBlock(),
      graviteeContainer: ContainerBlock,
    },
  }),
);
