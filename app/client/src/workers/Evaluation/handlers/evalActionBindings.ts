import { dataTreeEvaluator } from "./evalTree";
import { removeFunctions } from "@appsmith/workers/Evaluation/evaluationUtils";
import type { EvalWorkerSyncRequest } from "../types";

export default function (request: EvalWorkerSyncRequest) {
  const { data } = request;
  const { bindings, executionParams, queryParams } = data;
  if (!dataTreeEvaluator) {
    return { values: undefined, errors: [] };
  }

  const values = dataTreeEvaluator.evaluateActionBindings(bindings, {
    ...queryParams,
    ...executionParams,
  });

  const cleanValues = removeFunctions(values);

  const errors = dataTreeEvaluator.errors;
  dataTreeEvaluator.clearErrors();
  return { values: cleanValues, errors };
}
