package com.appsmith.server.solutions;

import com.appsmith.external.models.ActionConfiguration;
import com.appsmith.external.models.ActionDTO;
import com.appsmith.external.models.Datasource;
import com.appsmith.external.models.DatasourceConfiguration;
import com.appsmith.external.models.Policy;
import com.appsmith.server.acl.AclPermission;
import com.appsmith.server.domains.Application;
import com.appsmith.server.domains.PermissionGroup;
import com.appsmith.server.domains.Theme;
import com.appsmith.server.domains.User;
import com.appsmith.server.domains.Workspace;
import com.appsmith.server.dtos.PermissionGroupInfoDTO;
import com.appsmith.server.helpers.MockPluginExecutor;
import com.appsmith.server.helpers.PluginExecutorHelper;
import com.appsmith.server.helpers.UserUtils;
import com.appsmith.server.repositories.ApplicationRepository;
import com.appsmith.server.repositories.DatasourceRepository;
import com.appsmith.server.repositories.NewActionRepository;
import com.appsmith.server.repositories.NewPageRepository;
import com.appsmith.server.repositories.PermissionGroupRepository;
import com.appsmith.server.repositories.ThemeRepository;
import com.appsmith.server.repositories.UserRepository;
import com.appsmith.server.repositories.WorkspaceRepository;
import com.appsmith.server.services.ApplicationPageService;
import com.appsmith.server.services.ApplicationService;
import com.appsmith.server.services.DatasourceService;
import com.appsmith.server.services.LayoutActionService;
import com.appsmith.server.services.PermissionGroupService;
import com.appsmith.server.services.PluginService;
import com.appsmith.server.services.ThemeService;
import com.appsmith.server.services.UserService;
import com.appsmith.server.services.WorkspaceService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.appsmith.server.acl.AclPermission.APPLICATION_CREATE_PAGES;
import static com.appsmith.server.acl.AclPermission.CREATE_DATASOURCE_ACTIONS;
import static com.appsmith.server.acl.AclPermission.DELETE_ACTIONS;
import static com.appsmith.server.acl.AclPermission.DELETE_APPLICATIONS;
import static com.appsmith.server.acl.AclPermission.DELETE_DATASOURCES;
import static com.appsmith.server.acl.AclPermission.DELETE_PAGES;
import static com.appsmith.server.acl.AclPermission.EXECUTE_ACTIONS;
import static com.appsmith.server.acl.AclPermission.EXECUTE_DATASOURCES;
import static com.appsmith.server.acl.AclPermission.EXPORT_APPLICATIONS;
import static com.appsmith.server.acl.AclPermission.MAKE_PUBLIC_APPLICATIONS;
import static com.appsmith.server.acl.AclPermission.MANAGE_ACTIONS;
import static com.appsmith.server.acl.AclPermission.MANAGE_APPLICATIONS;
import static com.appsmith.server.acl.AclPermission.MANAGE_DATASOURCES;
import static com.appsmith.server.acl.AclPermission.MANAGE_PAGES;
import static com.appsmith.server.acl.AclPermission.MANAGE_THEMES;
import static com.appsmith.server.acl.AclPermission.PAGE_CREATE_PAGE_ACTIONS;
import static com.appsmith.server.acl.AclPermission.PUBLISH_APPLICATIONS;
import static com.appsmith.server.acl.AclPermission.READ_ACTIONS;
import static com.appsmith.server.acl.AclPermission.READ_APPLICATIONS;
import static com.appsmith.server.acl.AclPermission.READ_DATASOURCES;
import static com.appsmith.server.acl.AclPermission.READ_PAGES;
import static com.appsmith.server.acl.AclPermission.READ_THEMES;
import static com.appsmith.server.acl.AclPermission.READ_WORKSPACES;
import static com.appsmith.server.constants.FieldName.ADMINISTRATOR;
import static com.appsmith.server.constants.FieldName.APPLICATION_DEVELOPER;
import static com.appsmith.server.constants.FieldName.APPLICATION_VIEWER;
import static com.appsmith.server.constants.FieldName.DEVELOPER;
import static com.appsmith.server.constants.FieldName.VIEWER;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DirtiesContext
@Slf4j
public class ApplicationShareTest {

    @MockBean
    PluginExecutorHelper pluginExecutorHelper;
    @Autowired
    UserUtils userUtils;


    @Autowired
    ApplicationPageService applicationPageService;
    @Autowired
    ApplicationService applicationService;
    @Autowired
    DatasourceService datasourceService;
    @Autowired
    LayoutActionService layoutActionService;
    @Autowired
    PluginService pluginService;
    @Autowired
    PermissionGroupService permissionGroupService;
    @Autowired
    ThemeService themeService;
    @Autowired
    UserService userService;
    @Autowired
    WorkspaceService workspaceService;

    @Autowired
    ApplicationRepository applicationRepository;
    @Autowired
    DatasourceRepository datasourceRepository;
    @Autowired
    NewActionRepository newActionRepository;
    @Autowired
    NewPageRepository newPageRepository;
    @Autowired
    PermissionGroupRepository permissionGroupRepository;
    @Autowired
    ThemeRepository themeRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    WorkspaceRepository workspaceRepository;

    User apiUser = null;
    User testUser = null;

    @BeforeEach
    @WithUserDetails(value = "api_user")
    public void setup() {
        if (apiUser == null) {
            apiUser = userRepository.findByEmail("api_user").block();
        }
        if (testUser == null) {
            testUser = userRepository.findByEmail("usertest@usertest.com").block();
        }
    }

    @Test
    @WithUserDetails(value = "usertest@usertest.com")
    public void testGenerateDefaultRoleForTestUser_validateDevAppRole() {
        String testName = "testGenerateDefaultRoleForTestUser_validateDevAppRole";
        Mockito.when(pluginExecutorHelper.getPluginExecutor(Mockito.any())).thenReturn(Mono.just(new MockPluginExecutor()));
        String pluginId = pluginService.findByPackageName("restapi-plugin").block().getId();
        PermissionGroup instanceAdminRole = userUtils.getSuperAdminPermissionGroup().block();
        Workspace workspace = new Workspace();
        workspace.setName(testName);
        Workspace createdWorkspace = workspaceService.create(workspace, testUser, Boolean.TRUE).block();
        List<PermissionGroup> defaultWorkspaceRoles = permissionGroupService
                .findAllByIds(createdWorkspace.getDefaultPermissionGroups()).collectList().block();
        String adminRoleId = defaultWorkspaceRoles.stream().filter(role -> role.getName().startsWith(ADMINISTRATOR)).map(role -> role.getId()).findFirst().get();
        String devRoleId = defaultWorkspaceRoles.stream().filter(role -> role.getName().startsWith(DEVELOPER)).map(role -> role.getId()).findFirst().get();

        Application application = new Application();
        application.setName(testName);
        application.setWorkspaceId(createdWorkspace.getId());
        Application createdApplication = applicationPageService.createApplication(application).block();

        Theme systemTheme = themeRepository.getSystemThemeByName("Classic").block();
        Theme applicationTheme = themeService.persistCurrentTheme(createdApplication.getId(), null, systemTheme)
                .flatMap(persistedTheme -> themeService.updateTheme(createdApplication.getId(), null, persistedTheme))
                .block();

        Datasource datasource = new Datasource();
        datasource.setName(testName);
        DatasourceConfiguration datasourceConfiguration = new DatasourceConfiguration();
        datasourceConfiguration.setUrl("http://test.com");
        datasource.setDatasourceConfiguration(datasourceConfiguration);
        datasource.setWorkspaceId(workspace.getId());
        datasource.setPluginId(pluginId);
        Datasource createdDatasource = datasourceService.create(datasource).block();

        ActionDTO action = new ActionDTO();
        action.setName(testName);
        action.setWorkspaceId(createdWorkspace.getId());
        action.setPluginId(pluginId);
        action.setPageId(createdApplication.getPages().get(0).getId());
        ActionConfiguration actionConfiguration = new ActionConfiguration();
        actionConfiguration.setHttpMethod(HttpMethod.GET);
        action.setActionConfiguration(actionConfiguration);
        action.setDatasource(createdDatasource);

        ActionDTO createdActionBlock = layoutActionService.createSingleAction(action, Boolean.FALSE).block();

        PermissionGroup devApplicationRole = applicationService.createDefaultRole(createdApplication, APPLICATION_DEVELOPER).block();
        assertThat(devApplicationRole).isNotNull();
        assertThat(devApplicationRole.getName()).isEqualTo(APPLICATION_DEVELOPER + " - " + testName);
        assertThat(devApplicationRole.getDefaultDomainId()).isEqualTo(createdApplication.getId());
        assertThat(devApplicationRole.getDefaultDomainType()).isEqualTo(Application.class.getSimpleName());
        Optional<Policy> managePolicy = devApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.MANAGE_PERMISSION_GROUPS.getValue()))
                .findFirst();
        assertThat(managePolicy.isEmpty()).isTrue();
        Optional<Policy> readPolicy = devApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.READ_PERMISSION_GROUPS.getValue()))
                .findFirst();
        assertThat(readPolicy.isPresent()).isTrue();
        assertThat(readPolicy.get().getPermissionGroups()).isEqualTo(Set.of(instanceAdminRole.getId()));
        Optional<Policy> assignPolicy = devApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.ASSIGN_PERMISSION_GROUPS.getValue()))
                .findFirst();
        assertThat(assignPolicy.isPresent()).isTrue();
        assertThat(assignPolicy.get().getPermissionGroups())
                .isEqualTo(Set.of(devApplicationRole.getId(), instanceAdminRole.getId(), adminRoleId, devRoleId));
        Optional<Policy> unAssignPolicy = devApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.UNASSIGN_PERMISSION_GROUPS.getValue()))
                .findFirst();
        assertThat(unAssignPolicy.isPresent()).isTrue();
        assertThat(unAssignPolicy.get().getPermissionGroups())
                .isEqualTo(Set.of(devApplicationRole.getId(), instanceAdminRole.getId(), adminRoleId, devRoleId));
        Optional<Policy> readMembersPolicyDev = devApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.READ_PERMISSION_GROUP_MEMBERS.getValue()))
                .findFirst();
        assertThat(readMembersPolicyDev.isPresent()).isTrue();
        assertThat(readMembersPolicyDev.get().getPermissionGroups())
                .isEqualTo(Set.of(devApplicationRole.getId(), instanceAdminRole.getId(), adminRoleId, devRoleId));

        Set<Policy> applicationPolicies = applicationRepository.findById(createdApplication.getId()).block().getPolicies();
        Set<Policy> datasourcePolicies = datasourceRepository.findById(createdDatasource.getId()).block().getPolicies();
        Set<Policy> newPagePolicies = newPageRepository.findById(createdApplication.getPages().get(0).getId()).block().getPolicies();
        Set<Policy> newActionPolicies = newActionRepository.findById(createdActionBlock.getId()).block().getPolicies();
        Set<Policy> systemThemePolicies = themeRepository.findById(systemTheme.getId()).block().getPolicies();
        Set<Policy> applicationThemePolicies = themeRepository.findById(applicationTheme.getId()).block().getPolicies();
        Set<Policy> workspacePolicies = workspaceRepository.findById(createdWorkspace.getId()).block().getPolicies();

        applicationPolicies.forEach(policy -> {
            if (policy.getPermission().equals(EXPORT_APPLICATIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(devApplicationRole.getId());
            }
            if (policy.getPermission().equals(READ_APPLICATIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
            }
            if (policy.getPermission().equals(MANAGE_APPLICATIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
            }
            if (policy.getPermission().equals(DELETE_APPLICATIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
            }
            if (policy.getPermission().equals(PUBLISH_APPLICATIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(devApplicationRole.getId());
            }
            if (policy.getPermission().equals(MAKE_PUBLIC_APPLICATIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(devApplicationRole.getId());
            }
            if (policy.getPermission().equals(APPLICATION_CREATE_PAGES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
            }
        });

        datasourcePolicies.forEach(policy -> {
            if (policy.getPermission().equals(MANAGE_DATASOURCES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
            }
            if (policy.getPermission().equals(DELETE_DATASOURCES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
            }
            if (policy.getPermission().equals(EXECUTE_DATASOURCES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
            }
            if (policy.getPermission().equals(READ_DATASOURCES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
            }
            if (policy.getPermission().equals(CREATE_DATASOURCE_ACTIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
            }
        });

        newPagePolicies.forEach(policy -> {
            if (policy.getPermission().equals(MANAGE_PAGES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
            }
            if (policy.getPermission().equals(DELETE_PAGES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
            }
            if (policy.getPermission().equals(READ_PAGES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
            }
            if (policy.getPermission().equals(PAGE_CREATE_PAGE_ACTIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
            }
        });

        newActionPolicies.forEach(policy -> {
            if (policy.getPermission().equals(MANAGE_ACTIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
            }
            if (policy.getPermission().equals(DELETE_ACTIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
            }
            if (policy.getPermission().equals(READ_ACTIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
            }
            if (policy.getPermission().equals(EXECUTE_ACTIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
            }
        });

        systemThemePolicies.forEach(policy -> {
            if (policy.getPermission().equals(READ_THEMES.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(devApplicationRole.getId());
            }
        });

        applicationThemePolicies.forEach(policy -> {
            if (policy.getPermission().equals(READ_THEMES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
            }
            if (policy.getPermission().equals(MANAGE_THEMES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
            }
        });
        workspacePolicies.forEach(policy -> {
            if (policy.getPermission().equals(READ_WORKSPACES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
            } else {
                assertThat(policy.getPermissionGroups()).doesNotContain(devApplicationRole.getId());
            }
        });

    }

    @Test
    @WithUserDetails(value = "usertest@usertest.com")
    public void testGenerateDefaultRoleForTestUser_validateDevAppRoleWhenAppViewRoleExists() {
        String testName = "testGenerateDefaultRoleForTestUser_validateDevAppRoleWhenAppViewRoleExists";
        Mockito.when(pluginExecutorHelper.getPluginExecutor(Mockito.any())).thenReturn(Mono.just(new MockPluginExecutor()));
        String pluginId = pluginService.findByPackageName("restapi-plugin").block().getId();
        PermissionGroup instanceAdminRole = userUtils.getSuperAdminPermissionGroup().block();
        Workspace workspace = new Workspace();
        workspace.setName(testName);
        Workspace createdWorkspace = workspaceService.create(workspace, testUser, Boolean.TRUE).block();
        List<PermissionGroup> defaultWorkspaceRoles = permissionGroupService
                .findAllByIds(createdWorkspace.getDefaultPermissionGroups()).collectList().block();
        String adminRoleId = defaultWorkspaceRoles.stream().filter(role -> role.getName().startsWith(ADMINISTRATOR)).map(role -> role.getId()).findFirst().get();
        String devRoleId = defaultWorkspaceRoles.stream().filter(role -> role.getName().startsWith(DEVELOPER)).map(role -> role.getId()).findFirst().get();
        String viewRoleId = defaultWorkspaceRoles.stream().filter(role -> role.getName().startsWith(VIEWER)).map(role -> role.getId()).findFirst().get();

        Application application = new Application();
        application.setName(testName);
        application.setWorkspaceId(createdWorkspace.getId());
        Application createdApplication = applicationPageService.createApplication(application).block();

        Theme systemTheme = themeRepository.getSystemThemeByName("Classic").block();
        Theme applicationTheme = themeService.persistCurrentTheme(createdApplication.getId(), null, systemTheme)
                .flatMap(persistedTheme -> themeService.updateTheme(createdApplication.getId(), null, persistedTheme))
                .block();

        Datasource datasource = new Datasource();
        datasource.setName(testName);
        DatasourceConfiguration datasourceConfiguration = new DatasourceConfiguration();
        datasourceConfiguration.setUrl("http://test.com");
        datasource.setDatasourceConfiguration(datasourceConfiguration);
        datasource.setWorkspaceId(workspace.getId());
        datasource.setPluginId(pluginId);
        Datasource createdDatasource = datasourceService.create(datasource).block();

        ActionDTO action = new ActionDTO();
        action.setName(testName);
        action.setWorkspaceId(createdWorkspace.getId());
        action.setPluginId(pluginId);
        action.setPageId(createdApplication.getPages().get(0).getId());
        ActionConfiguration actionConfiguration = new ActionConfiguration();
        actionConfiguration.setHttpMethod(HttpMethod.GET);
        action.setActionConfiguration(actionConfiguration);
        action.setDatasource(createdDatasource);

        ActionDTO createdActionBlock = layoutActionService.createSingleAction(action, Boolean.FALSE).block();

        System.out.println("Create Viewer Role");
        PermissionGroup viewApplicationRole = applicationService.createDefaultRole(createdApplication, APPLICATION_VIEWER).block();
        System.out.println("Create Developer Role");
        assertThat(viewApplicationRole).isNotNull();
        PermissionGroup devApplicationRole = applicationService.createDefaultRole(createdApplication, APPLICATION_DEVELOPER).block();
        assertThat(devApplicationRole).isNotNull();
        assertThat(devApplicationRole.getName()).isEqualTo(APPLICATION_DEVELOPER + " - " + testName);
        assertThat(devApplicationRole.getDefaultDomainId()).isEqualTo(createdApplication.getId());
        assertThat(devApplicationRole.getDefaultDomainType()).isEqualTo(Application.class.getSimpleName());
        Optional<Policy> managePolicyDev = devApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.MANAGE_PERMISSION_GROUPS.getValue()))
                .findFirst();
        assertThat(managePolicyDev.isEmpty()).isTrue();
        Optional<Policy> readPolicyDev = devApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.READ_PERMISSION_GROUPS.getValue()))
                .findFirst();
        assertThat(readPolicyDev.isPresent()).isTrue();
        assertThat(readPolicyDev.get().getPermissionGroups()).isEqualTo(Set.of(instanceAdminRole.getId()));
        Optional<Policy> assignPolicyDev = devApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.ASSIGN_PERMISSION_GROUPS.getValue()))
                .findFirst();
        assertThat(assignPolicyDev.isPresent()).isTrue();
        assertThat(assignPolicyDev.get().getPermissionGroups())
                .isEqualTo(Set.of(devApplicationRole.getId(), instanceAdminRole.getId(), adminRoleId, devRoleId));
        Optional<Policy> unAssignPolicyDev = devApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.UNASSIGN_PERMISSION_GROUPS.getValue()))
                .findFirst();
        assertThat(unAssignPolicyDev.isPresent()).isTrue();
        assertThat(unAssignPolicyDev.get().getPermissionGroups())
                .isEqualTo(Set.of(devApplicationRole.getId(), instanceAdminRole.getId(), adminRoleId, devRoleId));
        Optional<Policy> readMembersPolicyDev = devApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.READ_PERMISSION_GROUP_MEMBERS.getValue()))
                .findFirst();
        assertThat(readMembersPolicyDev.isPresent()).isTrue();
        assertThat(readMembersPolicyDev.get().getPermissionGroups())
                .isEqualTo(Set.of(devApplicationRole.getId(), instanceAdminRole.getId(), adminRoleId, devRoleId));

        PermissionGroup updatedViewApplicationRole = permissionGroupRepository
                .findById(viewApplicationRole.getId(), Optional.empty()).block();
        assertThat(updatedViewApplicationRole).isNotNull();
        assertThat(updatedViewApplicationRole.getName()).isEqualTo(APPLICATION_VIEWER + " - " + testName);
        assertThat(updatedViewApplicationRole.getDefaultDomainId()).isEqualTo(createdApplication.getId());
        assertThat(updatedViewApplicationRole.getDefaultDomainType()).isEqualTo(Application.class.getSimpleName());
        Optional<Policy> managePolicyView = updatedViewApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.MANAGE_PERMISSION_GROUPS.getValue()))
                .findFirst();
        assertThat(managePolicyView.isEmpty()).isTrue();
        Optional<Policy> readPolicyView = updatedViewApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.READ_PERMISSION_GROUPS.getValue()))
                .findFirst();
        assertThat(readPolicyView.isPresent()).isTrue();
        assertThat(readPolicyView.get().getPermissionGroups()).isEqualTo(Set.of(instanceAdminRole.getId()));
        Optional<Policy> assignPolicyView = updatedViewApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.ASSIGN_PERMISSION_GROUPS.getValue()))
                .findFirst();
        assertThat(assignPolicyView.isPresent()).isTrue();
        assertThat(assignPolicyView.get().getPermissionGroups())
                .isEqualTo(Set.of(viewApplicationRole.getId(), devApplicationRole.getId(), instanceAdminRole.getId(), adminRoleId, devRoleId, viewRoleId));
        Optional<Policy> unAssignPolicyView = updatedViewApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.UNASSIGN_PERMISSION_GROUPS.getValue()))
                .findFirst();
        assertThat(unAssignPolicyView.isPresent()).isTrue();
        assertThat(unAssignPolicyView.get().getPermissionGroups())
                .isEqualTo(Set.of(viewApplicationRole.getId(), devApplicationRole.getId(), instanceAdminRole.getId(), adminRoleId, devRoleId, viewRoleId));
        Optional<Policy> readMembersPolicyView = updatedViewApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.READ_PERMISSION_GROUP_MEMBERS.getValue()))
                .findFirst();
        assertThat(readMembersPolicyView.isPresent()).isTrue();
        assertThat(readMembersPolicyView.get().getPermissionGroups())
                .isEqualTo(Set.of(viewApplicationRole.getId(), devApplicationRole.getId(), instanceAdminRole.getId(), adminRoleId, devRoleId, viewRoleId));

        Set<Policy> applicationPolicies = applicationRepository.findById(createdApplication.getId()).block().getPolicies();
        Set<Policy> datasourcePolicies = datasourceRepository.findById(createdDatasource.getId()).block().getPolicies();
        Set<Policy> newPagePolicies = newPageRepository.findById(createdApplication.getPages().get(0).getId()).block().getPolicies();
        Set<Policy> newActionPolicies = newActionRepository.findById(createdActionBlock.getId()).block().getPolicies();
        Set<Policy> systemThemePolicies = themeRepository.findById(systemTheme.getId()).block().getPolicies();
        Set<Policy> applicationThemePolicies = themeRepository.findById(applicationTheme.getId()).block().getPolicies();
        Set<Policy> workspacePolicies = workspaceRepository.findById(createdWorkspace.getId()).block().getPolicies();

        applicationPolicies.forEach(policy -> {
            if (policy.getPermission().equals(EXPORT_APPLICATIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(devApplicationRole.getId(), viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(READ_APPLICATIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId(), viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(MANAGE_APPLICATIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(DELETE_APPLICATIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(PUBLISH_APPLICATIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(devApplicationRole.getId(), viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(MAKE_PUBLIC_APPLICATIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(devApplicationRole.getId(), viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(APPLICATION_CREATE_PAGES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
        });

        datasourcePolicies.forEach(policy -> {
            if (policy.getPermission().equals(MANAGE_DATASOURCES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(DELETE_DATASOURCES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(EXECUTE_DATASOURCES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(READ_DATASOURCES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(CREATE_DATASOURCE_ACTIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
        });

        newPagePolicies.forEach(policy -> {
            if (policy.getPermission().equals(MANAGE_PAGES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(DELETE_PAGES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(READ_PAGES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId(), viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(PAGE_CREATE_PAGE_ACTIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
        });

        newActionPolicies.forEach(policy -> {
            if (policy.getPermission().equals(MANAGE_ACTIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(DELETE_ACTIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(READ_ACTIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(EXECUTE_ACTIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId(), viewApplicationRole.getId());
            }
        });

        systemThemePolicies.forEach(policy -> {
            if (policy.getPermission().equals(READ_THEMES.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(devApplicationRole.getId(), viewApplicationRole.getId());
            }
        });

        applicationThemePolicies.forEach(policy -> {
            if (policy.getPermission().equals(READ_THEMES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId(), viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(MANAGE_THEMES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
        });

        workspacePolicies.forEach(policy -> {
            if (policy.getPermission().equals(READ_WORKSPACES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId(), viewApplicationRole.getId());
            } else {
                assertThat(policy.getPermissionGroups()).doesNotContain(devApplicationRole.getId(), viewApplicationRole.getId());
            }
        });
    }


    @Test
    @WithUserDetails(value = "usertest@usertest.com")
    public void testGenerateDefaultRoleForTestUser_validateViewAppRole() {
        String testName = "testGenerateDefaultRoleForTestUser_validateViewAppRole";
        Mockito.when(pluginExecutorHelper.getPluginExecutor(Mockito.any())).thenReturn(Mono.just(new MockPluginExecutor()));
        String pluginId = pluginService.findByPackageName("restapi-plugin").block().getId();
        PermissionGroup instanceAdminRole = userUtils.getSuperAdminPermissionGroup().block();
        Workspace workspace = new Workspace();
        workspace.setName(testName);
        Workspace createdWorkspace = workspaceService.create(workspace, testUser, Boolean.TRUE).block();
        List<PermissionGroup> defaultWorkspaceRoles = permissionGroupService
                .findAllByIds(createdWorkspace.getDefaultPermissionGroups()).collectList().block();
        String adminRoleId = defaultWorkspaceRoles.stream().filter(role -> role.getName().startsWith(ADMINISTRATOR)).map(role -> role.getId()).findFirst().get();
        String devRoleId = defaultWorkspaceRoles.stream().filter(role -> role.getName().startsWith(DEVELOPER)).map(role -> role.getId()).findFirst().get();
        String viewRoleId = defaultWorkspaceRoles.stream().filter(role -> role.getName().startsWith(VIEWER)).map(role -> role.getId()).findFirst().get();

        Application application = new Application();
        application.setName(testName);
        application.setWorkspaceId(createdWorkspace.getId());
        Application createdApplication = applicationPageService.createApplication(application).block();

        Datasource datasource = new Datasource();
        datasource.setName(testName);
        DatasourceConfiguration datasourceConfiguration = new DatasourceConfiguration();
        datasourceConfiguration.setUrl("http://test.com");
        datasource.setDatasourceConfiguration(datasourceConfiguration);
        datasource.setWorkspaceId(workspace.getId());
        datasource.setPluginId(pluginId);
        Datasource createdDatasource = datasourceService.create(datasource).block();

        ActionDTO action = new ActionDTO();
        action.setName(testName);
        action.setWorkspaceId(createdWorkspace.getId());
        action.setPluginId(pluginId);
        action.setPageId(createdApplication.getPages().get(0).getId());
        ActionConfiguration actionConfiguration = new ActionConfiguration();
        actionConfiguration.setHttpMethod(HttpMethod.GET);
        action.setActionConfiguration(actionConfiguration);
        action.setDatasource(createdDatasource);

        ActionDTO createdActionBlock = layoutActionService.createSingleAction(action, Boolean.FALSE).block();

        PermissionGroup viewApplicationRole = applicationService.createDefaultRole(createdApplication, APPLICATION_VIEWER).block();
        assertThat(viewApplicationRole).isNotNull();
        assertThat(viewApplicationRole.getName()).isEqualTo(APPLICATION_VIEWER + " - " + testName);
        assertThat(viewApplicationRole.getDefaultDomainId()).isEqualTo(createdApplication.getId());
        assertThat(viewApplicationRole.getDefaultDomainType()).isEqualTo(Application.class.getSimpleName());
        Optional<Policy> managePolicy = viewApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.MANAGE_PERMISSION_GROUPS.getValue()))
                .findFirst();
        assertThat(managePolicy.isEmpty()).isTrue();
        Optional<Policy> readPolicy = viewApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.READ_PERMISSION_GROUPS.getValue()))
                .findFirst();
        assertThat(readPolicy.isPresent()).isTrue();
        assertThat(readPolicy.get().getPermissionGroups()).isEqualTo(Set.of(instanceAdminRole.getId()));
        Optional<Policy> assignPolicy = viewApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.ASSIGN_PERMISSION_GROUPS.getValue()))
                .findFirst();
        assertThat(assignPolicy.isPresent()).isTrue();
        assertThat(assignPolicy.get().getPermissionGroups()).isEqualTo(Set.of(viewApplicationRole.getId(), instanceAdminRole.getId(), adminRoleId, devRoleId, viewRoleId));
        Optional<Policy> unAssignPolicy = viewApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.UNASSIGN_PERMISSION_GROUPS.getValue()))
                .findFirst();
        assertThat(unAssignPolicy.isPresent()).isTrue();
        assertThat(unAssignPolicy.get().getPermissionGroups()).isEqualTo(Set.of(viewApplicationRole.getId(), instanceAdminRole.getId(), adminRoleId, devRoleId, viewRoleId));
        Optional<Policy> readMembersPolicy = viewApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.READ_PERMISSION_GROUP_MEMBERS.getValue()))
                .findFirst();
        assertThat(readMembersPolicy.isPresent()).isTrue();
        assertThat(readMembersPolicy.get().getPermissionGroups()).isEqualTo(Set.of(viewApplicationRole.getId(), instanceAdminRole.getId(), adminRoleId, devRoleId, viewRoleId));

        Set<Policy> applicationPolicies = applicationRepository.findById(createdApplication.getId()).block().getPolicies();
        Set<Policy> datasourcePolicies = datasourceRepository.findById(createdDatasource.getId()).block().getPolicies();
        Set<Policy> newPagePolicies = newPageRepository.findById(createdApplication.getPages().get(0).getId()).block().getPolicies();
        Set<Policy> newActionPolicies = newActionRepository.findById(createdActionBlock.getId()).block().getPolicies();
        Set<Policy> workspacePolicies = workspaceRepository.findById(createdWorkspace.getId()).block().getPolicies();

        applicationPolicies.forEach(policy -> {
            if (policy.getPermission().equals(EXPORT_APPLICATIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(READ_APPLICATIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(MANAGE_APPLICATIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(DELETE_APPLICATIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(PUBLISH_APPLICATIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(MAKE_PUBLIC_APPLICATIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(APPLICATION_CREATE_PAGES.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
        });

        datasourcePolicies.forEach(policy -> {
            if (policy.getPermission().equals(MANAGE_DATASOURCES.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(DELETE_DATASOURCES.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(EXECUTE_DATASOURCES.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(READ_DATASOURCES.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(CREATE_DATASOURCE_ACTIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
        });

        newPagePolicies.forEach(policy -> {
            if (policy.getPermission().equals(MANAGE_PAGES.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(DELETE_PAGES.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(READ_PAGES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(PAGE_CREATE_PAGE_ACTIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
        });

        newActionPolicies.forEach(policy -> {
            if (policy.getPermission().equals(MANAGE_ACTIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(DELETE_ACTIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(READ_ACTIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(EXECUTE_ACTIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(viewApplicationRole.getId());
            }
        });

        workspacePolicies.forEach(policy -> {
            if (policy.getPermission().equals(READ_WORKSPACES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(viewApplicationRole.getId());
            } else {
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
        });
    }

    @Test
    @WithUserDetails(value = "usertest@usertest.com")
    public void testGenerateDefaultRoleForTestUser_validateViewAppRoleWhenDevRoleExists() {
        String testName = "testGenerateDefaultRoleForTestUser_validateViewAppRoleWhenDevRoleCreated";
        Mockito.when(pluginExecutorHelper.getPluginExecutor(Mockito.any())).thenReturn(Mono.just(new MockPluginExecutor()));
        String pluginId = pluginService.findByPackageName("restapi-plugin").block().getId();
        PermissionGroup instanceAdminRole = userUtils.getSuperAdminPermissionGroup().block();
        Workspace workspace = new Workspace();
        workspace.setName(testName);
        Workspace createdWorkspace = workspaceService.create(workspace, testUser, Boolean.TRUE).block();
        List<PermissionGroup> defaultWorkspaceRoles = permissionGroupService
                .findAllByIds(createdWorkspace.getDefaultPermissionGroups()).collectList().block();
        String adminRoleId = defaultWorkspaceRoles.stream().filter(role -> role.getName().startsWith(ADMINISTRATOR)).map(role -> role.getId()).findFirst().get();
        String devRoleId = defaultWorkspaceRoles.stream().filter(role -> role.getName().startsWith(DEVELOPER)).map(role -> role.getId()).findFirst().get();
        String viewRoleId = defaultWorkspaceRoles.stream().filter(role -> role.getName().startsWith(VIEWER)).map(role -> role.getId()).findFirst().get();

        Application application = new Application();
        application.setName(testName);
        application.setWorkspaceId(createdWorkspace.getId());
        Application createdApplication = applicationPageService.createApplication(application).block();

        Datasource datasource = new Datasource();
        datasource.setName(testName);
        DatasourceConfiguration datasourceConfiguration = new DatasourceConfiguration();
        datasourceConfiguration.setUrl("http://test.com");
        datasource.setDatasourceConfiguration(datasourceConfiguration);
        datasource.setWorkspaceId(workspace.getId());
        datasource.setPluginId(pluginId);
        Datasource createdDatasource = datasourceService.create(datasource).block();

        ActionDTO action = new ActionDTO();
        action.setName(testName);
        action.setWorkspaceId(createdWorkspace.getId());
        action.setPluginId(pluginId);
        action.setPageId(createdApplication.getPages().get(0).getId());
        ActionConfiguration actionConfiguration = new ActionConfiguration();
        actionConfiguration.setHttpMethod(HttpMethod.GET);
        action.setActionConfiguration(actionConfiguration);
        action.setDatasource(createdDatasource);

        ActionDTO createdActionBlock = layoutActionService.createSingleAction(action, Boolean.FALSE).block();

        System.out.println("Create Dev Role");
        PermissionGroup devApplicationRole = applicationService.createDefaultRole(createdApplication, APPLICATION_DEVELOPER).block();
        assertThat(devApplicationRole).isNotNull();
        System.out.println("Create View Role");
        PermissionGroup viewApplicationRole = applicationService.createDefaultRole(createdApplication, APPLICATION_VIEWER).block();
        assertThat(viewApplicationRole).isNotNull();
        assertThat(viewApplicationRole.getName()).isEqualTo(APPLICATION_VIEWER + " - " + testName);
        assertThat(viewApplicationRole.getDefaultDomainId()).isEqualTo(createdApplication.getId());
        assertThat(viewApplicationRole.getDefaultDomainType()).isEqualTo(Application.class.getSimpleName());
        Optional<Policy> managePolicy = viewApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.MANAGE_PERMISSION_GROUPS.getValue()))
                .findFirst();
        assertThat(managePolicy.isEmpty()).isTrue();
        Optional<Policy> readPolicy = viewApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.READ_PERMISSION_GROUPS.getValue()))
                .findFirst();
        assertThat(readPolicy.isPresent()).isTrue();
        assertThat(readPolicy.get().getPermissionGroups()).isEqualTo(Set.of(instanceAdminRole.getId()));
        Optional<Policy> assignPolicy = viewApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.ASSIGN_PERMISSION_GROUPS.getValue()))
                .findFirst();
        assertThat(assignPolicy.isPresent()).isTrue();
        assertThat(assignPolicy.get().getPermissionGroups())
                .isEqualTo(Set.of(viewApplicationRole.getId(), devApplicationRole.getId(), instanceAdminRole.getId(), adminRoleId, devRoleId, viewRoleId));
        Optional<Policy> unAssignPolicy = viewApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.UNASSIGN_PERMISSION_GROUPS.getValue()))
                .findFirst();
        assertThat(unAssignPolicy.isPresent()).isTrue();
        assertThat(unAssignPolicy.get().getPermissionGroups())
                .isEqualTo(Set.of(viewApplicationRole.getId(), devApplicationRole.getId(), instanceAdminRole.getId(), adminRoleId, devRoleId, viewRoleId));

        Optional<Policy> readMembersPolicy = viewApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.READ_PERMISSION_GROUP_MEMBERS.getValue()))
                .findFirst();
        assertThat(readMembersPolicy.isPresent()).isTrue();
        assertThat(readMembersPolicy.get().getPermissionGroups())
                .isEqualTo(Set.of(viewApplicationRole.getId(), devApplicationRole.getId(), instanceAdminRole.getId(), adminRoleId, devRoleId, viewRoleId));

        Set<Policy> applicationPolicies = applicationRepository.findById(createdApplication.getId()).block().getPolicies();
        Set<Policy> datasourcePolicies = datasourceRepository.findById(createdDatasource.getId()).block().getPolicies();
        Set<Policy> newPagePolicies = newPageRepository.findById(createdApplication.getPages().get(0).getId()).block().getPolicies();
        Set<Policy> newActionPolicies = newActionRepository.findById(createdActionBlock.getId()).block().getPolicies();
        Set<Policy> workspacePolicies = workspaceRepository.findById(createdWorkspace.getId()).block().getPolicies();

        applicationPolicies.forEach(policy -> {
            if (policy.getPermission().equals(EXPORT_APPLICATIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(devApplicationRole.getId(), viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(READ_APPLICATIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId(), viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(MANAGE_APPLICATIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(DELETE_APPLICATIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(PUBLISH_APPLICATIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(devApplicationRole.getId(), viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(MAKE_PUBLIC_APPLICATIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).doesNotContain(devApplicationRole.getId(), viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(APPLICATION_CREATE_PAGES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
        });

        datasourcePolicies.forEach(policy -> {
            if (policy.getPermission().equals(MANAGE_DATASOURCES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(DELETE_DATASOURCES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(EXECUTE_DATASOURCES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(READ_DATASOURCES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(CREATE_DATASOURCE_ACTIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
        });

        newPagePolicies.forEach(policy -> {
            if (policy.getPermission().equals(MANAGE_PAGES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(DELETE_PAGES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(READ_PAGES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId(), viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(PAGE_CREATE_PAGE_ACTIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
        });

        newActionPolicies.forEach(policy -> {
            if (policy.getPermission().equals(MANAGE_ACTIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(DELETE_ACTIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(READ_ACTIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId());
                assertThat(policy.getPermissionGroups()).doesNotContain(viewApplicationRole.getId());
            }
            if (policy.getPermission().equals(EXECUTE_ACTIONS.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId(), viewApplicationRole.getId());
            }
        });

        workspacePolicies.forEach(policy -> {
            if (policy.getPermission().equals(READ_WORKSPACES.getValue())) {
                assertThat(policy.getPermissionGroups()).contains(devApplicationRole.getId(), viewApplicationRole.getId());
            } else {
                assertThat(policy.getPermissionGroups()).doesNotContain(devApplicationRole.getId(), viewApplicationRole.getId());
            }
        });
    }

    @Test
    @WithUserDetails(value = "usertest@usertest.com")
    public void testDeleteDefaultRoleForTestUser_deleteDevRole() {
        String testName = "testDeleteDefaultRoleForTestUser_deleteDevRole";
        Workspace workspace = new Workspace();
        workspace.setName(testName);
        Workspace createdWorkspace = workspaceService.create(workspace, testUser, Boolean.TRUE).block();

        Application application = new Application();
        application.setName(testName);
        application.setWorkspaceId(createdWorkspace.getId());
        Application createdApplication = applicationPageService.createApplication(application).block();

        PermissionGroup devApplicationRole = applicationService.createDefaultRole(createdApplication, APPLICATION_DEVELOPER).block();
        assertThat(devApplicationRole).isNotNull();

        applicationService.deleteDefaultRole(application, devApplicationRole).block();
        PermissionGroup deletedDevApplicationRole = permissionGroupRepository.findById(devApplicationRole.getId()).block();
        assertThat(deletedDevApplicationRole).isNull();
    }

    @Test
    @WithUserDetails(value = "usertest@usertest.com")
    public void testDeleteDefaultRoleForTestUser_deleteDevRoleWhenViewRolePresent() {
        String testName = "testDeleteDefaultRoleForTestUser_deleteDevRoleWhenViewRolePresent";
        PermissionGroup instanceAdminRole = userUtils.getSuperAdminPermissionGroup().block();
        Workspace workspace = new Workspace();
        workspace.setName(testName);
        Workspace createdWorkspace = workspaceService.create(workspace, testUser, Boolean.TRUE).block();
        List<PermissionGroup> defaultWorkspaceRoles = permissionGroupService
                .findAllByIds(createdWorkspace.getDefaultPermissionGroups()).collectList().block();
        String adminRoleId = defaultWorkspaceRoles.stream().filter(role -> role.getName().startsWith(ADMINISTRATOR)).map(role -> role.getId()).findFirst().get();
        String devRoleId = defaultWorkspaceRoles.stream().filter(role -> role.getName().startsWith(DEVELOPER)).map(role -> role.getId()).findFirst().get();
        String viewRoleId = defaultWorkspaceRoles.stream().filter(role -> role.getName().startsWith(VIEWER)).map(role -> role.getId()).findFirst().get();

        Application application = new Application();
        application.setName(testName);
        application.setWorkspaceId(createdWorkspace.getId());
        Application createdApplication = applicationPageService.createApplication(application).block();

        PermissionGroup devApplicationRole = applicationService.createDefaultRole(createdApplication, APPLICATION_DEVELOPER).block();
        assertThat(devApplicationRole).isNotNull();
        PermissionGroup viewApplicationRole = applicationService.createDefaultRole(createdApplication, APPLICATION_VIEWER).block();
        assertThat(viewApplicationRole).isNotNull();

        applicationService.deleteDefaultRole(application, devApplicationRole).block();
        PermissionGroup deletedDevApplicationRole = permissionGroupRepository.findById(devApplicationRole.getId()).block();
        assertThat(deletedDevApplicationRole).isNull();

        PermissionGroup updatedViewApplicationRole = permissionGroupRepository
                .findById(viewApplicationRole.getId(), Optional.empty()).block();
        assertThat(updatedViewApplicationRole).isNotNull();
        assertThat(updatedViewApplicationRole.getName()).isEqualTo(APPLICATION_VIEWER + " - " + testName);
        assertThat(updatedViewApplicationRole.getDefaultDomainId()).isEqualTo(createdApplication.getId());
        assertThat(updatedViewApplicationRole.getDefaultDomainType()).isEqualTo(Application.class.getSimpleName());
        Optional<Policy> managePolicyView = updatedViewApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.MANAGE_PERMISSION_GROUPS.getValue()))
                .findFirst();
        assertThat(managePolicyView.isEmpty()).isTrue();
        Optional<Policy> readPolicyView = updatedViewApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.READ_PERMISSION_GROUPS.getValue()))
                .findFirst();
        assertThat(readPolicyView.isPresent()).isTrue();
        assertThat(readPolicyView.get().getPermissionGroups()).isEqualTo(Set.of(instanceAdminRole.getId()));
        Optional<Policy> assignPolicyView = updatedViewApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.ASSIGN_PERMISSION_GROUPS.getValue()))
                .findFirst();
        assertThat(assignPolicyView.isPresent()).isTrue();
        assertThat(assignPolicyView.get().getPermissionGroups())
                .contains(viewApplicationRole.getId(), instanceAdminRole.getId(), adminRoleId, devRoleId, viewRoleId);
        Optional<Policy> unAssignPolicyView = updatedViewApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.UNASSIGN_PERMISSION_GROUPS.getValue()))
                .findFirst();
        assertThat(unAssignPolicyView.isPresent()).isTrue();
        assertThat(unAssignPolicyView.get().getPermissionGroups())
                .contains(viewApplicationRole.getId(), instanceAdminRole.getId(), adminRoleId, devRoleId, viewRoleId);
        Optional<Policy> readMembersPolicyView = updatedViewApplicationRole.getPolicies().stream()
                .filter(policy -> policy.getPermission().equals(AclPermission.READ_PERMISSION_GROUP_MEMBERS.getValue()))
                .findFirst();
        assertThat(readMembersPolicyView.isPresent()).isTrue();
        assertThat(readMembersPolicyView.get().getPermissionGroups())
                .contains(viewApplicationRole.getId(), instanceAdminRole.getId(), adminRoleId, devRoleId, viewRoleId);
    }

    @Test
    @WithUserDetails(value = "usertest@usertest.com")
    public void testDeleteDefaultRoleForTestUser_deleteViewRole() {
        String testName = "testDeleteDefaultRoleForTestUser_deleteViewRole";
        Workspace workspace = new Workspace();
        workspace.setName(testName);
        Workspace createdWorkspace = workspaceService.create(workspace, testUser, Boolean.TRUE).block();

        Application application = new Application();
        application.setName(testName);
        application.setWorkspaceId(createdWorkspace.getId());
        Application createdApplication = applicationPageService.createApplication(application).block();

        PermissionGroup viewApplicationRole = applicationService.createDefaultRole(createdApplication, APPLICATION_VIEWER).block();
        assertThat(viewApplicationRole).isNotNull();

        applicationService.deleteDefaultRole(application, viewApplicationRole).block();
        PermissionGroup deletedViewApplicationRole = permissionGroupRepository.findById(viewApplicationRole.getId()).block();
        assertThat(deletedViewApplicationRole).isNull();
    }

    @Test
    @WithUserDetails(value = "usertest@usertest.com")
    public void testFetchAllDefaultRolesTestUser_userHasWorkspaceAdminAccess() {
        String testName = "testFetchAllDefaultRolesTestUser_userHasWorkspaceAdminAccess";
        Workspace workspace = new Workspace();
        workspace.setName(testName);
        Workspace createdWorkspace = workspaceService.create(workspace, testUser, Boolean.TRUE).block();

        Application application = new Application();
        application.setName(testName);
        application.setWorkspaceId(createdWorkspace.getId());
        Application createdApplication = applicationPageService.createApplication(application).block();

        List<PermissionGroupInfoDTO> defaultRoleDescriptionDTOs = applicationService.fetchAllDefaultRoles(createdApplication.getId()).block();
        assertThat(defaultRoleDescriptionDTOs).hasSize(2);
        PermissionGroupInfoDTO developerRole = defaultRoleDescriptionDTOs.get(0);
        PermissionGroupInfoDTO viewerRole = defaultRoleDescriptionDTOs.get(1);
        assertThat(developerRole.getName()).startsWith(APPLICATION_DEVELOPER);
        assertThat(viewerRole.getName()).startsWith(APPLICATION_VIEWER);
    }

    @Test
    @WithUserDetails(value = "usertest@usertest.com")
    public void testFetchAllDefaultRolesTestUser_userHasWorkspaceDeveloperAccess() {
        String testName = "testFetchAllDefaultRolesTestUser_userHasWorkspaceDeveloperAccess";
        Workspace workspace = new Workspace();
        workspace.setName(testName);
        Workspace createdWorkspace = workspaceService.create(workspace, testUser, Boolean.TRUE).block();

        Application application = new Application();
        application.setName(testName);
        application.setWorkspaceId(createdWorkspace.getId());
        Application createdApplication = applicationPageService.createApplication(application).block();

        User user = new User();
        user.setEmail(testName);
        user.setPassword(testName);
        User newUser = userService.userCreate(user, false).block();

        List<PermissionGroup> defaultWorkspaceRoles = permissionGroupService
                .findAllByIds(createdWorkspace.getDefaultPermissionGroups()).collectList().block();
        PermissionGroup workspaceAdminRole = defaultWorkspaceRoles.stream()
                .filter(role -> role.getName().startsWith(ADMINISTRATOR))
                .findFirst().get();
        PermissionGroup workspaceDevRole = defaultWorkspaceRoles.stream()
                .filter(role -> role.getName().startsWith(DEVELOPER))
                .findFirst().get();

        permissionGroupService.assignToUser(workspaceAdminRole, newUser).block();
        permissionGroupService.assignToUser(workspaceDevRole, testUser).block();
        permissionGroupService.unassignFromUser(workspaceAdminRole, testUser).block();

        List<PermissionGroupInfoDTO> defaultRoleDescriptionDTOs = applicationService.fetchAllDefaultRoles(createdApplication.getId()).block();
        assertThat(defaultRoleDescriptionDTOs).hasSize(2);
        PermissionGroupInfoDTO developerRole = defaultRoleDescriptionDTOs.get(0);
        PermissionGroupInfoDTO viewerRole = defaultRoleDescriptionDTOs.get(1);
        assertThat(developerRole.getName()).startsWith(APPLICATION_DEVELOPER);
        assertThat(viewerRole.getName()).startsWith(APPLICATION_VIEWER);
    }

    @Test
    @WithUserDetails(value = "usertest@usertest.com")
    public void testFetchAllDefaultRolesTestUser_userHasWorkspaceViewerAccess() {
        String testName = "testFetchAllDefaultRolesTestUser_userHasWorkspaceViewerAccess";
        Workspace workspace = new Workspace();
        workspace.setName(testName);
        Workspace createdWorkspace = workspaceService.create(workspace, testUser, Boolean.TRUE).block();

        Application application = new Application();
        application.setName(testName);
        application.setWorkspaceId(createdWorkspace.getId());
        Application createdApplication = applicationPageService.createApplication(application).block();

        User user = new User();
        user.setEmail(testName);
        user.setPassword(testName);
        User newUser = userService.userCreate(user, false).block();

        List<PermissionGroup> defaultWorkspaceRoles = permissionGroupService
                .findAllByIds(createdWorkspace.getDefaultPermissionGroups()).collectList().block();
        PermissionGroup workspaceAdminRole = defaultWorkspaceRoles.stream()
                .filter(role -> role.getName().startsWith(ADMINISTRATOR))
                .findFirst().get();
        PermissionGroup workspaceViewRole = defaultWorkspaceRoles.stream()
                .filter(role -> role.getName().startsWith(VIEWER))
                .findFirst().get();

        permissionGroupService.assignToUser(workspaceAdminRole, newUser).block();
        permissionGroupService.assignToUser(workspaceViewRole, testUser).block();
        permissionGroupService.unassignFromUser(workspaceAdminRole, testUser).block();

        List<PermissionGroupInfoDTO> defaultRoleDescriptionDTOs = applicationService.fetchAllDefaultRoles(createdApplication.getId()).block();
        assertThat(defaultRoleDescriptionDTOs).hasSize(1);
        PermissionGroupInfoDTO viewerRole = defaultRoleDescriptionDTOs.get(0);
        assertThat(viewerRole.getName()).startsWith(APPLICATION_VIEWER);
    }

    @Test
    @WithUserDetails(value = "usertest@usertest.com")
    public void testFetchAllDefaultRolesTestUser_userHasApplicationDeveloperAccess() {
        String testName = "testFetchAllDefaultRolesTestUser_userHasApplicationDeveloperAccess";
        Workspace workspace = new Workspace();
        workspace.setName(testName);
        Workspace createdWorkspace = workspaceService.create(workspace, testUser, Boolean.TRUE).block();

        Application application = new Application();
        application.setName(testName);
        application.setWorkspaceId(createdWorkspace.getId());
        Application createdApplication = applicationPageService.createApplication(application).block();

        User user = new User();
        user.setEmail(testName);
        user.setPassword(testName);
        User newUser = userService.userCreate(user, false).block();

        List<PermissionGroup> defaultWorkspaceRoles = permissionGroupService
                .findAllByIds(createdWorkspace.getDefaultPermissionGroups()).collectList().block();
        PermissionGroup workspaceAdminRole = defaultWorkspaceRoles.stream()
                .filter(role -> role.getName().startsWith(ADMINISTRATOR))
                .findFirst().get();
        PermissionGroup applicationDevRole = applicationService.createDefaultRole(createdApplication, APPLICATION_DEVELOPER).block();

        permissionGroupService.assignToUser(workspaceAdminRole, newUser).block();
        permissionGroupService.assignToUser(applicationDevRole, testUser).block();
        permissionGroupService.unassignFromUser(workspaceAdminRole, testUser).block();

        List<PermissionGroupInfoDTO> defaultRoleDescriptionDTOs = applicationService.fetchAllDefaultRoles(createdApplication.getId()).block();
        assertThat(defaultRoleDescriptionDTOs).hasSize(2);
        PermissionGroupInfoDTO developerRole = defaultRoleDescriptionDTOs.get(0);
        PermissionGroupInfoDTO viewerRole = defaultRoleDescriptionDTOs.get(1);
        assertThat(developerRole.getName()).startsWith(APPLICATION_DEVELOPER);
        assertThat(viewerRole.getName()).startsWith(APPLICATION_VIEWER);
    }

    @Test
    @WithUserDetails(value = "usertest@usertest.com")
    public void testFetchAllDefaultRolesTestUser_userHasApplicationViewerAccess() {
        String testName = "testFetchAllDefaultRolesTestUser_userHasApplicationViewerAccess";
        Workspace workspace = new Workspace();
        workspace.setName(testName);
        Workspace createdWorkspace = workspaceService.create(workspace, testUser, Boolean.TRUE).block();

        Application application = new Application();
        application.setName(testName);
        application.setWorkspaceId(createdWorkspace.getId());
        Application createdApplication = applicationPageService.createApplication(application).block();

        User user = new User();
        user.setEmail(testName);
        user.setPassword(testName);
        User newUser = userService.userCreate(user, false).block();

        List<PermissionGroup> defaultWorkspaceRoles = permissionGroupService
                .findAllByIds(createdWorkspace.getDefaultPermissionGroups()).collectList().block();
        PermissionGroup workspaceAdminRole = defaultWorkspaceRoles.stream()
                .filter(role -> role.getName().startsWith(ADMINISTRATOR))
                .findFirst().get();
        PermissionGroup applicationViewRole = applicationService.createDefaultRole(createdApplication, APPLICATION_VIEWER).block();

        permissionGroupService.assignToUser(workspaceAdminRole, newUser).block();
        permissionGroupService.assignToUser(applicationViewRole, testUser).block();
        permissionGroupService.unassignFromUser(workspaceAdminRole, testUser).block();

        List<PermissionGroupInfoDTO> defaultRoleDescriptionDTOs = applicationService.fetchAllDefaultRoles(createdApplication.getId()).block();
        assertThat(defaultRoleDescriptionDTOs).hasSize(1);
        PermissionGroupInfoDTO viewerRole = defaultRoleDescriptionDTOs.get(0);
        assertThat(viewerRole.getName()).startsWith(APPLICATION_VIEWER);
    }

    @Test
    @WithUserDetails(value = "usertest@usertest.com")
    public void testFetchAllDefaultRolesTestUser_userHasWorkspaceDeveloperApplicationViewerAccess() {
        String testName = "testFetchAllDefaultRolesTestUser_userHasWorkspaceDeveloperApplicationViewerAccess";
        Workspace workspace = new Workspace();
        workspace.setName(testName);
        Workspace createdWorkspace = workspaceService.create(workspace, testUser, Boolean.TRUE).block();

        Application application = new Application();
        application.setName(testName);
        application.setWorkspaceId(createdWorkspace.getId());
        Application createdApplication = applicationPageService.createApplication(application).block();

        User user = new User();
        user.setEmail(testName);
        user.setPassword(testName);
        User newUser = userService.userCreate(user, false).block();

        List<PermissionGroup> defaultWorkspaceRoles = permissionGroupService
                .findAllByIds(createdWorkspace.getDefaultPermissionGroups()).collectList().block();
        PermissionGroup workspaceAdminRole = defaultWorkspaceRoles.stream()
                .filter(role -> role.getName().startsWith(ADMINISTRATOR))
                .findFirst().get();
        PermissionGroup workspaceDevRole = defaultWorkspaceRoles.stream()
                .filter(role -> role.getName().startsWith(DEVELOPER))
                .findFirst().get();
        PermissionGroup applicationViewRole = applicationService.createDefaultRole(createdApplication, APPLICATION_VIEWER).block();

        permissionGroupService.assignToUser(workspaceAdminRole, newUser).block();
        permissionGroupService.assignToUser(workspaceDevRole, testUser).block();
        permissionGroupService.assignToUser(applicationViewRole, testUser).block();
        permissionGroupService.unassignFromUser(workspaceAdminRole, testUser).block();

        List<PermissionGroupInfoDTO> defaultRoleDescriptionDTOs = applicationService.fetchAllDefaultRoles(createdApplication.getId()).block();
        assertThat(defaultRoleDescriptionDTOs).hasSize(2);
        PermissionGroupInfoDTO developerRole = defaultRoleDescriptionDTOs.get(0);
        PermissionGroupInfoDTO viewerRole = defaultRoleDescriptionDTOs.get(1);
        assertThat(developerRole.getName()).startsWith(APPLICATION_DEVELOPER);
        assertThat(viewerRole.getName()).startsWith(APPLICATION_VIEWER);
    }

    @Test
    @WithUserDetails(value = "usertest@usertest.com")
    public void testFetchAllDefaultRolesTestUser_userHasWorkspaceViewerApplicationDeveloperAccess() {
        String testName = "testFetchAllDefaultRolesTestUser_userHasWorkspaceViewerApplicationDeveloperAccess";
        Workspace workspace = new Workspace();
        workspace.setName(testName);
        Workspace createdWorkspace = workspaceService.create(workspace, testUser, Boolean.TRUE).block();

        Application application = new Application();
        application.setName(testName);
        application.setWorkspaceId(createdWorkspace.getId());
        Application createdApplication = applicationPageService.createApplication(application).block();

        User user = new User();
        user.setEmail(testName);
        user.setPassword(testName);
        User newUser = userService.userCreate(user, false).block();

        List<PermissionGroup> defaultWorkspaceRoles = permissionGroupService
                .findAllByIds(createdWorkspace.getDefaultPermissionGroups()).collectList().block();
        PermissionGroup workspaceAdminRole = defaultWorkspaceRoles.stream()
                .filter(role -> role.getName().startsWith(ADMINISTRATOR))
                .findFirst().get();
        PermissionGroup workspaceViewRole = defaultWorkspaceRoles.stream()
                .filter(role -> role.getName().startsWith(VIEWER))
                .findFirst().get();
        PermissionGroup applicationDevRole = applicationService.createDefaultRole(createdApplication, APPLICATION_DEVELOPER).block();

        permissionGroupService.assignToUser(workspaceAdminRole, newUser).block();
        permissionGroupService.assignToUser(workspaceViewRole, testUser).block();
        permissionGroupService.assignToUser(applicationDevRole, testUser).block();
        permissionGroupService.unassignFromUser(workspaceAdminRole, testUser).block();

        List<PermissionGroupInfoDTO> defaultRoleDescriptionDTOs = applicationService.fetchAllDefaultRoles(createdApplication.getId()).block();
        assertThat(defaultRoleDescriptionDTOs).hasSize(2);
        PermissionGroupInfoDTO developerRole = defaultRoleDescriptionDTOs.get(0);
        PermissionGroupInfoDTO viewerRole = defaultRoleDescriptionDTOs.get(1);
        assertThat(developerRole.getName()).startsWith(APPLICATION_DEVELOPER);
        assertThat(viewerRole.getName()).startsWith(APPLICATION_VIEWER);
    }
}
