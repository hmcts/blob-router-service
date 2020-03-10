package uk.gov.hmcts.reform.blobrouter.services;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.EnvelopeRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.singleton;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class NewEnvelopesFinder {

    private static final Logger logger = getLogger(NewEnvelopesFinder.class);

    private final EnvelopeRepository envelopeRepository;

    private final ServiceConfiguration serviceConfig;

    private final Duration timeInterval;

    private static final String CRIME_CONTAINER = "crime";

    public NewEnvelopesFinder(
        EnvelopeRepository envelopeRepository,
        ServiceConfiguration serviceConfig,
        @Value("${scheduling.task.check-new-envelopes.time-interval}") Duration timeInterval
    ) {
        Validate.isTrue(timeInterval != null, "Time interval is required");
        this.envelopeRepository = envelopeRepository;
        this.serviceConfig = serviceConfig;
        this.timeInterval = timeInterval;
    }

    public void checkNewCftEnvelopesCreated() {
        checkNewEnvelopesInContainers(getCftContainers(), "CFT");
    }

    public void checkNewCrimeEnvelopesCreated() {
        if (serviceConfig.getEnabledSourceContainers().contains(CRIME_CONTAINER)) {
            checkNewEnvelopesInContainers(singleton(CRIME_CONTAINER), "Crime");
        } else {
            logger.info(
                "Not checking for new envelopes in {} container because container is disabled", CRIME_CONTAINER
            );
        }
    }

    private void checkNewEnvelopesInContainers(Set<String> containers, String containersGroupName) {
        Instant toDateTime = Instant.now();
        Instant fromDateTime = toDateTime.minus(timeInterval);

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
