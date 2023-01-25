import { checkLiquibaseScripts } from "./check-liquibase-scripts";
import { checkPRSize } from "./check-PR-size";

checkLiquibaseScripts();
checkPRSize();