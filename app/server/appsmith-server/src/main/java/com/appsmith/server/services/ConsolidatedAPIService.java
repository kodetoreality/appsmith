package com.appsmith.server.services;

import com.appsmith.server.domains.ApplicationMode;
import com.appsmith.server.dtos.ConsolidatedAPIResponseDTO;
import reactor.core.publisher.Mono;

public interface ConsolidatedAPIService {
    Mono<ConsolidatedAPIResponseDTO> getConsolidatedInfoForPageLoad(String pageId, String applicationId,
                                                                    String branchName, ApplicationMode mode, Boolean migrateDsl);
}