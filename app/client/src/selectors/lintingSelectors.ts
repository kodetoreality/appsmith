import type { AppState } from "@appsmith/reducers";
import { get } from "lodash";
import type { LintError } from "widgets/types";
const emptyLint: LintError[] = [];

export const getEntityLintErrors = (state: AppState, path?: string) => {
  if (!path) return emptyLint;
  return get(state.linting.errors, path, emptyLint);
};
