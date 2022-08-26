import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Link, useHistory, useParams } from "react-router-dom";
import styled from "styled-components";
import debounce from "lodash/debounce";
import { getCurrentUser } from "selectors/usersSelectors";
// import { OrgUser } from "constants/orgConstants";
// import { getAllUsers } from "selectors/organizationSelectors";
// import { fetchUsersForOrg, fetchRolesForOrg } from "actions/orgActions";
import { Listing } from "./Listing";
import ProfileImage from "pages/common/ProfileImage";
import { Toaster, Variant } from "components/ads";
import { MenuItemProps } from "design-system";
import { PageHeader } from "./PageHeader";
import { BottomSpace } from "pages/Settings/components";
import { HighlightText } from "./helpers/HighlightText";
import { UserEdit } from "./UserEdit";
import { AclWrapper } from "./components";
import FormDialogComponent from "components/editorComponents/form/FormDialogComponent";
import WorkspaceInviteUsersForm from "pages/workspace/WorkspaceInviteUsersForm";
import { adminSettingsCategoryUrl } from "RouteBuilder";
import { SettingCategories } from "@appsmith/pages/AdminSettings/config/types";
import { ReduxActionTypes } from "@appsmith/constants/ReduxActionConstants";
import { deleteAclUser, getUserById } from "@appsmith/actions/aclActions";
import { getAllAclUsers } from "@appsmith/selectors/aclSelectors";
import {
  createMessage,
  SHOW_LESS_GROUPS,
  SHOW_MORE_GROUPS,
} from "@appsmith/constants/messages";

export const CellContainer = styled.div`
  display: flex;
  align-items: baseline;

  .user-icons {
    margin-right 8px;
    cursor: initial;

    span {
      color: var(--appsmith-color-black-0);
    }
  }
`;

export const GroupWrapper = styled.div``;

export const MoreGroups = styled.div`
  font-size: 12px;
  line-height: 16px;
  color: var(--appsmith-color-black-700);
  margin-top: 8px;
  &:hover {
    cursor: pointer;
    text-decoration: underline;
  }
`;

export const AllGroups = styled.div`
  display: flex;
  flex-direction: column;
  > div {
    margin: 8px 0;

    &:first-child {
      margin-top: 0;
    }

    &:last-child {
      margin-bottom: 0;
    }
  }
`;

export const ShowLess = styled.div`
  font-size: 12px;
  line-height: 16px;
  color: var(--appsmith-color-black-700);
  &:hover {
    cursor: pointer;
    text-decoration: underline;
  }
`;

export type User = {
  isCurrentUser: boolean;
  allGroups: Array<string>;
  allRoles: Array<string>;
  userId: string;
  username: string;
  name: string;
  roleName?: string;
  isDeleting: boolean;
  isChangingRole: boolean;
};

export const allUsers: User[] = [
  {
    isChangingRole: false,
    isCurrentUser: true,
    isDeleting: false,
    name: "Ankita Kinger",
    // roleName: "Administrator + 2 more",
    allGroups: ["Administrator", "Test_Admin", "HR_Admin"],
    allRoles: ["Administrator-PG", "Test_Admin-PG", "HR_Admin-PG"],
    username: "techak@appsmith.com",
    userId: "123",
  },
  {
    isChangingRole: false,
    isCurrentUser: false,
    isDeleting: false,
    name: "Sangy Sivan",
    // roleName: "App Viewer + 1 more",
    allGroups: ["App Viewer", "HR_Admin"],
    allRoles: ["App Viewer-PG", "HR_Admin-PG"],
    username: "sangy@appsmith.com",
    userId: "456",
  },
  {
    isChangingRole: false,
    isCurrentUser: false,
    isDeleting: false,
    name: "SS Sivan",
    // roleName: "App Viewer + 1 more",
    allGroups: ["App Viewer", "HR_Admin"],
    allRoles: ["App Viewer-PG", "HR_Admin-PG"],
    username: "sangy123@appsmith.com",
    userId: "789",
  },
];

export function UserListing() {
  const dispatch = useDispatch();

  useEffect(() => {
    dispatch({ type: ReduxActionTypes.FETCH_ACL_USERS });
  }, []);

  const aclUsers = useSelector(getAllAclUsers);
  const currentUser = useSelector(getCurrentUser);
  const history = useHistory();
  const params = useParams() as any;
  const selectedUserId = params?.selected;
  const selectedUser = allUsers.find((user) => user.userId === selectedUserId);

  const userTableData = allUsers.map((user) => ({
    ...user,
    isCurrentUser: user.username === currentUser?.username,
  }));

  const [data, setData] = useState<User[]>([]);
  const [searchValue, setSearchValue] = useState("");
  const [showModal, setShowModal] = useState(false);
  const [selectedUserIdProp, setSelectedUserIdProp] = useState("");

  useEffect(() => {
    setData(aclUsers);
  }, [aclUsers]);

  const onDeleteHanlder = (userId: string) => {
    dispatch(deleteAclUser(userId));
    const updatedData = data.filter((user) => {
      return user.userId !== userId;
    });
    setData(updatedData);
    Toaster.show({
      text: "User deleted successfully",
      variant: Variant.success,
    });
  };

  const onSelectUser = (userId: string) => {
    setSelectedUserIdProp(userId);
    userId && dispatch(getUserById({ id: userId }));
  };

  const columns = [
    {
      Header: `User (${data.length})`,
      accessor: "username",
      Cell: function UserCell(cellProps: any) {
        return (
          <Link
            data-testid="acl-user-listing-link"
            onClick={() => onSelectUser(cellProps.cell.row.original.userId)}
            to={adminSettingsCategoryUrl({
              category: SettingCategories.USER_LISTING,
              selected: cellProps.cell.row.original.userId,
            })}
          >
            <CellContainer data-testid="user-listing-userCell">
              <ProfileImage
                className="user-icons"
                size={20}
                source={`/api/v1/users/photo/${cellProps.cell.row.values.username}`}
                userName={cellProps.cell.row.values.username}
              />
              <HighlightText
                highlight={searchValue}
                text={cellProps.cell.row.values.username}
              />
            </CellContainer>
          </Link>
        );
      },
    },
    {
      Header: "Roles",
      accessor: "allRoles",
      Cell: function RoleCell(cellProps: any) {
        const [showAllGroups, setShowAllGroups] = useState(false);

        return (
          <CellContainer data-testid="user-listing-rolesCell">
            {showAllGroups ? (
              <AllGroups>
                {cellProps.cell.row.values.allRoles.map((group: any) => (
                  <div key={group}>{group}</div>
                ))}
                <ShowLess
                  data-testid="t--show-less"
                  onClick={() => setShowAllGroups(false)}
                >
                  {createMessage(SHOW_LESS_GROUPS)}
                </ShowLess>
              </AllGroups>
            ) : (
              <GroupWrapper>
                {cellProps.cell.row.values.allRoles[0]}
                {cellProps.cell.row.values.allRoles[0].length < 40 ? (
                  <>
                    , {cellProps.cell.row.values.allRoles[1]}
                    {cellProps.cell.row.values.allRoles.length > 2 && (
                      <MoreGroups
                        data-testid="t--show-more"
                        onClick={() => setShowAllGroups(true)}
                      >
                        {createMessage(
                          SHOW_MORE_GROUPS,
                          cellProps.cell.row.values.allRoles.length - 2,
                        )}
                      </MoreGroups>
                    )}
                  </>
                ) : (
                  cellProps.cell.row.values.allRoles.length > 1 && (
                    <MoreGroups
                      data-testid="t--show-more"
                      onClick={() => setShowAllGroups(true)}
                    >
                      {createMessage(
                        SHOW_MORE_GROUPS,
                        cellProps.cell.row.values.allRoles.length - 1,
                      )}
                    </MoreGroups>
                  )
                )}
              </GroupWrapper>
            )}
          </CellContainer>
        );
      },
    },
    {
      Header: "Groups",
      accessor: "allGroups",
      Cell: function GroupCell(cellProps: any) {
        const [showAllGroups, setShowAllGroups] = useState(false);

        return (
          <CellContainer data-testid="user-listing-groupCell">
            {showAllGroups ? (
              <AllGroups>
                {cellProps.cell.row.values.allGroups.map((group: any) => (
                  <div key={group}>{group}</div>
                ))}
                <ShowLess
                  data-testid="t--show-less"
                  onClick={() => setShowAllGroups(false)}
                >
                  {createMessage(SHOW_LESS_GROUPS)}
                </ShowLess>
              </AllGroups>
            ) : (
              <GroupWrapper>
                {cellProps.cell.row.values.allGroups[0]}
                {cellProps.cell.row.values.allGroups[0].length < 40 ? (
                  <>
                    , {cellProps.cell.row.values.allGroups[1]}
                    {cellProps.cell.row.values.allGroups.length > 2 && (
                      <MoreGroups
                        data-testid="t--show-more"
                        onClick={() => setShowAllGroups(true)}
                      >
                        {createMessage(
                          SHOW_MORE_GROUPS,
                          cellProps.cell.row.values.allGroups.length - 2,
                        )}
                      </MoreGroups>
                    )}
                  </>
                ) : (
                  cellProps.cell.row.values.allGroups.length > 1 && (
                    <MoreGroups
                      data-testid="t--show-more"
                      onClick={() => setShowAllGroups(true)}
                    >
                      {createMessage(
                        SHOW_MORE_GROUPS,
                        cellProps.cell.row.values.allGroups.length - 1,
                      )}
                    </MoreGroups>
                  )
                )}
              </GroupWrapper>
            )}
          </CellContainer>
        );
      },
    },
  ];

  const listMenuItems: MenuItemProps[] = [
    {
      label: "edit",
      className: "edit-menu-item",
      icon: "edit-underline",
      onSelect: (e: React.MouseEvent, userId: string) => {
        if (userId) {
          setSelectedUserIdProp(userId);
          history.push(`/settings/users/${userId}`);
        }
      },
      text: "Edit Groups",
    },
    {
      label: "delete",
      className: "delete-menu-item",
      icon: "delete-blank",
      onSelect: (e: React.MouseEvent, key: string) => {
        onDeleteHanlder(key);
      },
      text: "Delete User",
    },
  ];

  const pageMenuItems: MenuItemProps[] = [
    {
      icon: "book-line",
      className: "documentation-page-menu-item",
      onSelect: () => {
        /*console.log("hello onSelect")*/
      },
      text: "Documentation",
    },
  ];

  const onButtonClick = () => {
    setShowModal(true);
  };

  const onSearch = debounce((search: string) => {
    if (search && search.trim().length > 0) {
      setSearchValue(search);
      const results =
        userTableData &&
        userTableData.filter((user) =>
          user.username?.toLocaleUpperCase().includes(search),
        );
      setData(results);
    } else {
      setSearchValue("");
      setData(userTableData);
    }
  }, 300);

  return (
    <AclWrapper data-testid="user-listing-wrapper">
      {selectedUser ? (
        <UserEdit
          data-testid="acl-user-edit"
          onDelete={onDeleteHanlder}
          searchPlaceholder="Search users"
          selectedUser={selectedUser}
          selectedUserId={selectedUserIdProp}
        />
      ) : (
        <>
          <PageHeader
            buttonText="Add Users"
            data-testid="acl-user-listing-pageheader"
            onButtonClick={onButtonClick}
            onSearch={onSearch}
            pageMenuItems={pageMenuItems}
            searchPlaceholder="Search Users"
          />
          <Listing
            columns={columns}
            data={data}
            data-testid="acl-user-listing"
            keyAccessor="userId"
            listMenuItems={listMenuItems}
          />
          <FormDialogComponent
            Form={WorkspaceInviteUsersForm}
            canOutsideClickClose
            customProps={{
              isAclFlow: true,
              disableEmailSetup: true,
              disableManageUsers: true,
              disableUserList: true,
              isMultiSelectDropdown: true,
            }}
            data-testid="acl-user-listing-form"
            isOpen={showModal}
            onClose={() => setShowModal(false)}
            title={`Invite Users`}
            trigger
          />
        </>
      )}
      <BottomSpace />
    </AclWrapper>
  );
}
