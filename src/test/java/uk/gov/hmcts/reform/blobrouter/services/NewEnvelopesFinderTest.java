package uk.gov.hmcts.reform.blobrouter.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.EnvelopeRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import javax.validation.ClockProvider;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
public class NewEnvelopesFinderTest {

    @Mock
    private EnvelopeRepository envelopeRepository;

    @Mock
    private ServiceConfiguration serviceConfiguration;

    @MockBean
    private ClockProvider clockProvider;

    private NewEnvelopesFinder envelopesFinder;

    @Test
    void should_check_new_envelopes_in_cft_containers() {
        // given
        envelopesFinder = newEnvelopeFinderWithBusinessHours();

        List<String> containers = asList("c1", "c2", "c3", "crime", "xyz");
        Set<String> cftContainers = Set.of("c1", "c2", "c3", "xyz");

        given(serviceConfiguration.getEnabledSourceContainers())
            .willReturn(containers);

        given(envelopeRepository.getEnvelopesCount(
            eq(cftContainers), any(), any()
        )).willReturn(1);

        // when
        envelopesFinder.checkNewCftEnvelopesCreated();

        // then
        verify(serviceConfiguration).getEnabledSourceContainers();
        verify(envelopeRepository).getEnvelopesCount(eq(cftContainers), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"crime", "pcq"})
    void should_check_new_envelopes_in_the_container_when_the_container_is_enabled(String container) {
        // given
        envelopesFinder = newEnvelopeFinderWithBusinessHours();
        given(serviceConfiguration.getEnabledSourceContainers()).willReturn(singletonList(container));
        given(envelopeRepository.getEnvelopesCount(
            eq(singleton(container)), any(), any()
        )).willReturn(1);

        // when
        envelopesFinder.checkNewEnvelopesCreatedInContainer(container, container.toUpperCase());

        // then
        verify(serviceConfiguration).getEnabledSourceContainers();
        verify(envelopeRepository).getEnvelopesCount(eq(singleton(container)), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"crime", "pcq"})
    void should_not_check_new_envelopes_in_the_container_when_the_container_is_not_enabled(String container) {
        // given
        envelopesFinder = newEnvelopeFinderWithBusinessHours();
        given(serviceConfiguration.getEnabledSourceContainers()).willReturn(asList("c1", "c2"));

        // when
        envelopesFinder.checkNewEnvelopesCreatedInContainer(container, container.toUpperCase());

        // then
        verify(serviceConfiguration).getEnabledSourceContainers();
        verifyNoInteractions(envelopeRepository);
    }

    @Test
    void should_throw_exception_when_time_interval_is_null() {
        assertThat(catchThrowable(
            () -> new NewEnvelopesFinder(envelopeRepository, serviceConfiguration, null, clockProvider))
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Time interval is required");
    }

    @Test
    void should_not_check_new_envelopes_in_cft_container_before_business_hours() {
        // given
        ZonedDateTime dateTime = ZonedDateTime.of(
            LocalDateTime.of(2020, 3, 10, 8, 0, 0), ZoneId.systemDefault() // before business hours
        );
        when(clockProvider.getClock())
            .thenReturn(Clock.fixed(dateTime.toInstant(), ZoneId.systemDefault()));
        envelopesFinder = new NewEnvelopesFinder(
            envelopeRepository, serviceConfiguration, Duration.parse("PT10M"), clockProvider
        );

        // when
        envelopesFinder.checkNewCftEnvelopesCreated();

        // then
        verifyNoInteractions(serviceConfiguration, envelopeRepository);
    }

    @Test
    void should_throw_exception_when_container_value_is_null() {
        // given
        envelopesFinder = new NewEnvelopesFinder(
            envelopeRepository,
            serviceConfiguration,
            Duration.parse("PT10M"),
            clockProvider
        );

        // when
        assertThat(catchThrowable(
            () -> envelopesFinder.checkNewEnvelopesCreatedInContainer(null, null))
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'container' value is required");

        // then
        verifyNoInteractions(serviceConfiguration);
        verifyNoInteractions(envelopeRepository);
    }

    @Test
    void should_throw_exception_when_container_value_is_empty() {
        // given
        envelopesFinder = new NewEnvelopesFinder(
            envelopeRepository,
            serviceConfiguration,
            Duration.parse("PT10M"),
            clockProvider
        );

        // when
        assertThat(catchThrowable(
            () -> envelopesFinder.checkNewEnvelopesCreatedInContainer(" ", null))
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'container' value is required");

        // then
        verifyNoInteractions(serviceConfiguration);
        verifyNoInteractions(envelopeRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"crime", "pcq"})
    void should_not_check_new_envelopes_in_the_container_after_business_hours(String container) {
        // given
        ZonedDateTime dateTime = ZonedDateTime.of(
            LocalDateTime.of(2020, 3, 10, 19, 0, 0), ZoneId.systemDefault() // after business hours
        );
        when(clockProvider.getClock())
            .thenReturn(Clock.fixed(dateTime.toInstant(), ZoneId.systemDefault()));
        envelopesFinder = new NewEnvelopesFinder(
            envelopeRepository, serviceConfiguration, Duration.parse("PT10M"), clockProvider
        );

        // when
        envelopesFinder.checkNewEnvelopesCreatedInContainer(container, container.toUpperCase());

        // then
        verifyNoInteractions(serviceConfiguration, envelopeRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"crime", "pcq"})
    void should_not_check_new_envelopes_for_the_container_on_weekend(String container) {
        // given
        ZonedDateTime dateTime = ZonedDateTime.of(
            LocalDateTime.of(2020, 3, 8, 11, 0, 0), ZoneId.systemDefault() // weekend (sunday)
        );
        when(clockProvider.getClock())
            .thenReturn(Clock.fixed(dateTime.toInstant(), ZoneId.systemDefault()));
        envelopesFinder = new NewEnvelopesFinder(
            envelopeRepository, serviceConfiguration, Duration.parse("PT10M"), clockProvider
        );

        // when
        envelopesFinder.checkNewEnvelopesCreatedInContainer(container, container.toUpperCase());

        // then
        verifyNoInteractions(serviceConfiguration, envelopeRepository);
    }

    private NewEnvelopesFinder newEnvelopeFinderWithBusinessHours() {
        ZonedDateTime dateTime = ZonedDateTime.of(
            LocalDateTime.of(2020, 3, 10, 10, 0, 0), ZoneId.systemDefault() // business hours
        );
        when(clockProvider.getClock())
            .thenReturn(Clock.fixed(dateTime.toInstant(), ZoneId.systemDefault()));
        return new NewEnvelopesFinder(
            envelopeRepository, serviceConfiguration, Duration.parse("PT10M"), clockProvider
        );
    }

}
