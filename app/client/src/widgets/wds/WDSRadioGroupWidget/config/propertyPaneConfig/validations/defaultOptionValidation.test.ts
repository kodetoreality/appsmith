// Generated by CodiumAI
import _ from "lodash";
import { defaultOptionValidation } from "./defaultOptionValidation";
describe("code snippet", () => {
  // Validates a string value against an array of options
  it("should return isValid as true when value is a string present in options", () => {
    const value = "option1";
    const props = { options: [{ value: "option1" }, { value: "option2" }] };

    const result = defaultOptionValidation(value, props, _);

    expect(result.isValid).toBe(true);
    expect(result.parsed).toBe(value);
  });

  // Returns isValid as false when value is an object
  it("should return isValid as false when value is an object", () => {
    const value = { key: "value" };
    const props = { options: [{ value: "option1" }, { value: "option2" }] };

    const result = defaultOptionValidation(value, props, _);

    expect(result.isValid).toBe(false);
    expect(result.parsed).toBe(JSON.stringify(value, null, 2));
    expect(result?.messages?.[0]?.name).toBe("TypeError");
    expect(result?.messages?.[0]?.message).toBe(
      "This value does not evaluate to type: string or number",
    );
  });
});
