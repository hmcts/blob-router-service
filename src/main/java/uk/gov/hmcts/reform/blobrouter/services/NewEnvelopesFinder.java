package uk.gov.hmcts.reform.blobrouter.services;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepository;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.singleton;
import static org.slf4j.LoggerFactory.getLogger;

public class NewEnvelopesFinder {

    private static final Logger logger = getLogger(NewEnvelopesFinder.class);

    private final EnvelopeRepository envelopeRepository;

    private final ServiceConfiguration serviceConfig;

    private static final String CRIME_CONTAINER = "crime";

    public NewEnvelopesFinder(
        EnvelopeRepository envelopeRepository,
        ServiceConfiguration serviceConfig
    ) {
        this.envelopeRepository = envelopeRepository;
        this.serviceConfig = serviceConfig;
    }

    public void checkNewCftEnvelopesCreated() {
        checkNewEnvelopesInContainers(getCftContainers(), "CFT");
    }

    public void checkNewCrimeEnvelopesCreated() {
        if (serviceConfig.getEnabledSourceContainers().contains(CRIME_CONTAINER)) {
            checkNewEnvelopesInContainers(singleton(CRIME_CONTAINER), "CRIME");
        } else {
            logger.info(
                "Not checking for new envelopes in {} container because container is disabled", CRIME_CONTAINER
            );
        }
    }

    private void checkNewEnvelopesInContainers(Set<String> containers, String containersGroupName) {
        Instant toDateTime = Instant.now();
        Instant fromDateTime = toDateTime.minus(1, HOURS); //TODO: read duration from config

        Integer envelopesCount = envelopeRepository.getEnvelopesCount(containers, fromDateTime, toDateTime);

        if (envelopesCount == 0) {
            logger.info(
                "No Envelopes created in {} between {} and {}", containersGroupName, fromDateTime, toDateTime
            );
        }
    }

    private Set<String> getCftContainers() {
        return serviceConfig.getEnabledSourceContainers()
            .stream()
            .filter(container -> !StringUtils.equals(CRIME_CONTAINER, container))
            .collect(Collectors.toSet());
    }

}
