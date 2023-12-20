package com.appsmith.server.actioncollections.moduleentity;

import com.appsmith.external.models.Policy;
import com.appsmith.server.acl.PolicyGenerator;
import com.appsmith.server.domains.Action;
import com.appsmith.server.domains.ActionCollection;
import com.appsmith.server.domains.Module;
import com.appsmith.server.dtos.ActionCollectionDTO;
import com.appsmith.server.helpers.ModuleConsumable;
import com.appsmith.server.modules.moduleentity.ModulePublicEntityService;
import com.appsmith.server.modules.permissions.ModulePermissionChecker;
import com.appsmith.server.services.LayoutCollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ActionCollectionModulePublicEntityServiceImpl extends ActionCollectionModulePublicEntityServiceCECompatible
        implements ModulePublicEntityService<ActionCollection> {
    private final ModulePermissionChecker modulePermissionChecker;
    private final PolicyGenerator policyGenerator;
    private final LayoutCollectionService layoutCollectionService;

    @Override
    public Mono<ModuleConsumable> createPublicEntity(
            String workspaceId, Module module, ModuleConsumable moduleConsumable) {
        return this.createModuleActionCollection(
                Optional.of(workspaceId), module.getId(), (ActionCollectionDTO) moduleConsumable, true);
    }

    private Mono<ModuleConsumable> createModuleActionCollection(
            Optional<String> workspaceIdOptional,
            String moduleId,
            ActionCollectionDTO actionCollectionDTO,
            boolean isPublic) {
        return modulePermissionChecker
                .checkIfCreateExecutableAllowedAndReturnModuleAndWorkspaceId(moduleId, workspaceIdOptional)
                .flatMap(tuple -> {
                    Module module = tuple.getT1();
                    String workspaceId = tuple.getT2();
                    ActionCollection moduleActionCollection = JSModuleEntityHelper.generateActionCollectionDomain(
                            moduleId, workspaceId, isPublic, actionCollectionDTO);
                    Set<Policy> childActionCollectionPolicies =
                            policyGenerator.getAllChildPolicies(module.getPolicies(), Module.class, Action.class);
                    moduleActionCollection.setPolicies(childActionCollectionPolicies);

                    return layoutCollectionService
                            .createCollection(moduleActionCollection)
                            .map(collectionDTO -> (ModuleConsumable) collectionDTO);
                });
    }

    @Override
    public Mono<Object> getPublicEntitySettingsForm(String moduleId) {
        return Mono.just(List.of());
    }
}