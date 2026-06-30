import { BlockNoteSchema, defaultBlockSpecs } from '@blocknote/core';
import { ApiListBlock } from './ApiListBlock/ApiListBlock';
import { BannerBlock } from './BannerBlock/BannerBlock';
import { CardBlock } from './CardBlock/CardBlock';
import { ButtonBlock } from './ButtonBlock/ButtonBlock';
import { HtmlBlock } from './HtmlBlock/HtmlBlock';
import { MarkdownBlock } from './MarkdownBlock/MarkdownBlock';
import { SectionBlock } from './SectionBlock/SectionBlock';
import { ContainerBlock } from './ContainerBlock/ContainerBlock';
import { ColumnBlock, ColumnListBlock } from './MultiColumnBlock/multi-column-blocks';
import './MultiColumnBlock/multi-column.module.scss';

export const schema = BlockNoteSchema.create({
  blockSpecs: {
    ...defaultBlockSpecs,
    columnList: ColumnListBlock,
    column: ColumnBlock,
    graviteeApiList: ApiListBlock(),
    graviteeBanner: BannerBlock(),
    graviteeCard: CardBlock(),
    graviteeButton: ButtonBlock(),
    graviteeHtml: HtmlBlock(),
    graviteeMarkdown: MarkdownBlock(),
    graviteeSection: SectionBlock(),
    graviteeContainer: ContainerBlock,
  },
});
