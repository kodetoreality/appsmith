package com.appsmith.server.controllers;

import com.appsmith.server.constants.Url;
import com.appsmith.server.controllers.ce.ApplicationControllerCE;
import com.appsmith.server.dtos.PermissionGroupInfoDTO;
import com.appsmith.server.dtos.ResponseDTO;
import com.appsmith.server.services.ApplicationPageService;
import com.appsmith.server.services.ApplicationService;
import com.appsmith.server.services.ApplicationSnapshotService;
import com.appsmith.server.services.ThemeService;
import com.appsmith.server.solutions.ApplicationFetcher;
import com.appsmith.server.solutions.ApplicationForkingService;
import com.appsmith.server.solutions.ImportExportApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(Url.APPLICATION_URL)
public class ApplicationController extends ApplicationControllerCE {

    public ApplicationController(ApplicationService service,
                                 ApplicationPageService applicationPageService,
                                 ApplicationFetcher applicationFetcher,
                                 ApplicationForkingService applicationForkingService,
                                 ImportExportApplicationService importExportApplicationService,
                                 ThemeService themeService,
                                 ApplicationSnapshotService applicationSnapshotService) {

        super(service, applicationPageService, applicationFetcher, applicationForkingService,
                importExportApplicationService, themeService, applicationSnapshotService);

    }

    @GetMapping("/{applicationId}/roles")
    public Mono<ResponseDTO<List<PermissionGroupInfoDTO>>> fetchAllDefaultRoles(@PathVariable String applicationId) {
        log.debug("Fetching all default accessible roles for application id: {}", applicationId);
        Mono<List<PermissionGroupInfoDTO>> roleDescriptionDTOsMono = service.fetchAllDefaultRoles(applicationId);
        return roleDescriptionDTOsMono
                .map(roleDescriptionDTOs -> new ResponseDTO<>(HttpStatus.OK.value(), roleDescriptionDTOs, null));
    }
}
