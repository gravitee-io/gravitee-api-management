import { IMonacoRange } from "../../gravitee-monaco-wrapper/monaco-facade";
import { copyCodeSuggestions } from "./copy-code/copy-code.suggestions";
import { buttonSuggestions } from "./button/button.suggestions";
import { cardSuggestions } from "./card/card.suggestions";
import { imageSuggestions } from "./image/image.suggestions";
import { gridSuggestions, gridCellSuggestions } from "./grid/grid.suggestions";
import { latestApisSuggestions } from "./latest-apis/latest-apis.suggestions";

export const componentLibrarySuggestions = (range: IMonacoRange, needsOpeningTag: boolean = false) => [
  ...copyCodeSuggestions(range, needsOpeningTag),
  ...buttonSuggestions(range, needsOpeningTag),
  ...cardSuggestions(range, needsOpeningTag),
  ...imageSuggestions(range, needsOpeningTag),
  ...gridSuggestions(range, needsOpeningTag),
  ...gridCellSuggestions(range, needsOpeningTag),
  ...latestApisSuggestions(range, needsOpeningTag),
];