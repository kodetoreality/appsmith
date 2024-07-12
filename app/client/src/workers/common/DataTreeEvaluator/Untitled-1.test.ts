// Generated by CodiumAI

import DependencyMap from "entities/DependencyMap";
import DataTreeEvaluator from ".";
import { CrashingError } from "@appsmith/workers/Evaluation/evaluationUtils";

describe("sortDependencies", () => {
  // Returns sorted dependencies when there are no cyclic dependencies
  it("should return sorted dependencies when there are no cyclic dependencies", () => {
    const dependencyMap = new DependencyMap();
    dependencyMap.addNodes({
      "entity1.property1": true,
      "entity1.property2": true,
      "entity2.property1": true,
      "entity2.property2": true,
    });
    dependencyMap.addDependency("entity1.property1", ["entity1.property2"]);
    dependencyMap.addDependency("entity1.property2", ["entity2.property2"]);

    const dataTreeEvaluator = new DataTreeEvaluator({});
    const sortedDependencies =
      dataTreeEvaluator.sortDependencies(dependencyMap);

    expect(sortedDependencies).toEqual([
      "entity2.property2",
      "entity1.property2",
      "entity1.property1",
    ]);
  });

  // Handles cyclic dependencies involving multiple nodes
  it("should throw an error when there are cyclic dependencies involving multiple nodes", () => {
    const dependencyMap = new DependencyMap();
    dependencyMap.addNodes({
      "entity1.property1": true,
      "entity1.property2": true,
      "entity2.property1": true,
      "entity2.property2": true,
    });
    dependencyMap.addDependency("entity1.property1", ["entity1.property2"]);
    dependencyMap.addDependency("entity1.property2", ["entity2.property1"]);
    dependencyMap.addDependency("entity2.property1", ["entity1.property1"]); // Creates a cycle

    const dataTreeEvaluator = new DataTreeEvaluator({});

    expect(() => {
      dataTreeEvaluator.sortDependencies(dependencyMap);
    }).toThrow(CrashingError);
  });
});
