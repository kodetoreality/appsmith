export * from "ce/mocks/handlers";

import { rest } from "msw";
import mockAclUsers from "./mockJsons/mockResponseAclUsers.json";
import mockAclUserGroups from "./mockJsons/mockResponseAcluserGroups.json";
import mockAclPermissionGroups from "./mockJsons/mockResponseAclPermissionGroups.json";
import { handlers as CEHandlers } from "ce/mocks/handlers";

export const handlers = [
  rest.get("/api/testMockApi", (req, res, ctx) => {
    return res(ctx.status(200), ctx.json({ data: { enviroment: "EE" } }));
  }),
  rest.get("/mockUsers", (req, res, ctx) => {
    return res(ctx.status(200), ctx.delay(500), ctx.json(mockAclUsers));
  }),
  rest.get("/mockUsers/:userId", async (req, res, ctx) => {
    const { userId } = await req.params;
    if (userId) {
      const user = mockAclUsers.data.find(
        (user: any) => user?.userId === userId,
      );
      return res(
        ctx.status(200),
        ctx.delay(500),
        ctx.json({ responseMeta: { status: 200, success: true }, data: user }),
      );
    }
  }),
  rest.get("/mockGroups", (req, res, ctx) => {
    return res(ctx.status(200), ctx.delay(500), ctx.json(mockAclUserGroups));
  }),
  rest.get("/mockRoles", (req, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.delay(500),
      ctx.json(mockAclPermissionGroups),
    );
  }),
  ...CEHandlers,
];
