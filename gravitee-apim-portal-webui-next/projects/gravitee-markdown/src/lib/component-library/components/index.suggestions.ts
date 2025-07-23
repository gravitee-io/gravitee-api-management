import { IRange } from "monaco-editor";
import { copyCodeSuggestions } from "./copy-code/copy-code.suggestions";
import { buttonSuggestions } from "./button/button.suggestions";
import { cardSuggestions } from "./card/card.suggestions";
import { imageSuggestions } from "./image/image.suggestions";
import { gridSuggestions, gridCellSuggestions } from "./grid/grid.suggestions";

export const componentLibrarySuggestions = (range: IRange, needsOpeningTag: boolean = false) => [
  ...copyCodeSuggestions(range, needsOpeningTag),
  ...buttonSuggestions(range, needsOpeningTag),
  ...cardSuggestions(range, needsOpeningTag),
  ...imageSuggestions(range, needsOpeningTag),
  ...gridSuggestions(range, needsOpeningTag),
  ...gridCellSuggestions(range, needsOpeningTag),
];