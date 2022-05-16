package uk.gov.hmcts.reform.blobrouter.services;

import com.azure.storage.common.StorageSharedKeyCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.config.StorageConfigItem;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.TeamEnvelope;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.TeamContainerProcessor;

import java.util.List;

@Service
public class TeamEnvelopeService {

    private static final Logger logger = LoggerFactory.getLogger(SasTokenGeneratorService.class);

    private final StorageSharedKeyCredential storageSharedKeyCredential;
    private final ServiceConfiguration serviceConfiguration;
    private final TeamContainerProcessor teamContainerProcessor;

    public TeamEnvelopeService(
        StorageSharedKeyCredential storageSharedKeyCredential,
        ServiceConfiguration serviceConfiguration,
        TeamContainerProcessor teamContainerProcessor
    ) {
        this.storageSharedKeyCredential = storageSharedKeyCredential;
        this.serviceConfiguration = serviceConfiguration;
        this.teamContainerProcessor = teamContainerProcessor;
    }

    public List<TeamEnvelope> getEnvelopes(String teamId) {
        var storageConfiguration = this.serviceConfiguration.getStorageConfig();
        StorageConfigItem teamConfig = storageConfiguration.get(teamId);
        if (!teamConfig.isEnabled()) {
            logger.info(
                "Not checking for new envelopes in {} container because container is disabled", teamId
            );
        }

        return teamContainerProcessor.leaseAndGetEnvelopes(teamId);
    }
}
