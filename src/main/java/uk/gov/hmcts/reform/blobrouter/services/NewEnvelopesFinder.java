package uk.gov.hmcts.reform.blobrouter.services;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.EnvelopeRepository;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ClockProvider;

import static java.util.Collections.singleton;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * The `NewEnvelopesFinder` class in Java checks for new envelopes created in specified containers during business hours
 * based on certain criteria.
 */
@Component
public class NewEnvelopesFinder {

    private static final Logger logger = getLogger(NewEnvelopesFinder.class);

    private final EnvelopeRepository envelopeRepository;

    private final ServiceConfiguration serviceConfig;

    private final Duration timeInterval;

    private static final List<String> nonCftContainers = List.of("crime", "pcq");

    private final Clock clock;

    private static final int START_HOUR = 10;
    private static final int END_HOUR = 18;
    private static final List<DayOfWeek> WEEKEND = Arrays.asList(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    public NewEnvelopesFinder(
        EnvelopeRepository envelopeRepository,
        ServiceConfiguration serviceConfig,
        @Value("${scheduling.task.check-new-envelopes.time-interval}") Duration timeInterval,
        ClockProvider clockProvider
    ) {
        Validate.isTrue(timeInterval != null, "Time interval is required");
        this.envelopeRepository = envelopeRepository;
        this.serviceConfig = serviceConfig;
        this.timeInterval = timeInterval;
        this.clock = clockProvider.getClock();
    }

    public void checkNewCftEnvelopesCreated() {
        if (isCurrentTimeInBusinessHours(clock)) {
            checkNewEnvelopesInContainers(getCftContainers(), "CFT");
        }
    }

    /**
     * The function checks for new envelopes created in a specified container during business hours if the container is
     * enabled.
     *
     * @param container The `container` parameter is a String that represents the name or identifier of a specific
     *      container where envelopes are stored or managed.
     * @param containerGroup The `containerGroup` parameter is used to specify the group to which the
     *                       container belongs. It is passed as an argument to the
     *                       `checkNewEnvelopesCreatedInContainer` method along with the `container`
     *                       parameter.
     *                       The method then checks if the specified `container` is enabled and if the
     */
    public void checkNewEnvelopesCreatedInContainer(String container, String containerGroup) {
        Assert.hasText(container, "'container' value is required");
        if (isCurrentTimeInBusinessHours(clock)) {
            if (serviceConfig.getEnabledSourceContainers().contains(container)) {
                checkNewEnvelopesInContainers(singleton(container), containerGroup);
            } else {
                logger.info(
                    "Not checking for new envelopes in {} container because container is disabled", container
                );
            }
        }
    }

    /**
     * The function checks for new envelopes in containers within a specified time
     * interval and logs a message if none are found.
     *
     * @param containers A set of container IDs where you want to check for new envelopes.
     * @param containersGroupName containersGroupName is a String that represents the name of a group of containers.
     */
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

    /**
     * The function `getCftContainers` returns a set of enabled source containers that are not present in the
     * `nonCftContainers` set.
     *
     * @return A Set of Strings containing the enabled source containers that are
     *      not present in the nonCftContainers set.
     */
    private Set<String> getCftContainers() {
        return serviceConfig.getEnabledSourceContainers()
            .stream()
            .filter(container -> !nonCftContainers.contains(container))
            .collect(Collectors.toSet());
    }

    /**
     * The function `isCurrentTimeInBusinessHours` checks if the current time, based on a provided clock, falls within
     * business hours (10am-6pm) on weekdays.
     *
     * @param clock The `clock` parameter in the `isCurrentTimeInBusinessHours` method is an
     *              instance of the `Clock` class. It is used to provide the current time for
     *              the method to determine if it falls within the business hours.
     * @return The method `isCurrentTimeInBusinessHours` returns a boolean value indicating
     *      whether the current time, as per the provided clock, falls within the business hours criteria.
     */
    private boolean isCurrentTimeInBusinessHours(Clock clock) {
        LocalDateTime now = LocalDateTime.now(clock);
        return !WEEKEND.contains(now.getDayOfWeek()) // not weekend
            && now.getHour() >= START_HOUR && now.getHour() <= END_HOUR; // between 10am-6pm
    }
}
