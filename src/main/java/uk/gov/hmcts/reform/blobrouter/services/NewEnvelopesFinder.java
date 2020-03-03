package uk.gov.hmcts.reform.blobrouter.services;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.singleton;
import static org.slf4j.LoggerFactory.getLogger;

public class NewEnvelopesFinder {

    private static final Logger logger = getLogger(NewEnvelopesFinder.class);

    private final EnvelopeService envelopeService;

    private final ServiceConfiguration serviceConfig;

    private static final String CRIME_CONTAINER = "crime";

    public NewEnvelopesFinder(
        EnvelopeService envelopeService,
        ServiceConfiguration serviceConfig
    ) {
        this.envelopeService = envelopeService;
        this.serviceConfig = serviceConfig;
    }

    public void checkNewCftEnvelopesCreated() {
        checkNewEnvelopesInContainers(getCftContainers());
    }

    public void checkNewCrimeEnvelopesCreated() {
        checkNewEnvelopesInContainers(singleton(CRIME_CONTAINER));
    }

    private void checkNewEnvelopesInContainers(Set<String> singleton) {
        Instant toDateTime = Instant.now();
        Instant fromDateTime = toDateTime.minus(1, HOURS); //TODO: read duration from config

        Integer envelopesCount = envelopeService.getEnvelopesCount(
            singleton, fromDateTime, toDateTime
        );

        if (envelopesCount == 0) {
            logger.info("No Envelopes created in the last hour");
        }
    }

    private Set<String> getCftContainers() {
        return serviceConfig.getEnabledSourceContainers()
            .stream()
            .filter(container -> !StringUtils.equals(CRIME_CONTAINER, container))
            .collect(Collectors.toSet());
    }

}
