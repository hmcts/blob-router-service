package uk.gov.hmcts.reform.blobrouter.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Test
    void should_check_new_envelopes_in_crime_container_when_crime_is_enabled() {
        // given
        envelopesFinder = newEnvelopeFinderWithBusinessHours();
        given(serviceConfiguration.getEnabledSourceContainers()).willReturn(singletonList("crime"));
        given(envelopeRepository.getEnvelopesCount(
            eq(singleton("crime")), any(), any()
        )).willReturn(1);

        // when
        envelopesFinder.checkNewCrimeEnvelopesCreated();

        // then
        verify(serviceConfiguration).getEnabledSourceContainers();
        verify(envelopeRepository).getEnvelopesCount(eq(singleton("crime")), any(), any());
    }

    @Test
    void should_not_check_new_envelopes_in_crime_container_when_crime_is_not_enabled() {
        // given
        envelopesFinder = newEnvelopeFinderWithBusinessHours();
        given(serviceConfiguration.getEnabledSourceContainers()).willReturn(asList("c1", "c2"));

        // when
        envelopesFinder.checkNewCrimeEnvelopesCreated();

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
    void should_not_check_new_envelopes_in_crime_container_after_business_hours() {
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
        envelopesFinder.checkNewCrimeEnvelopesCreated();

        // then
        verifyNoInteractions(serviceConfiguration, envelopeRepository);
    }

    @Test
    void should_not_check_new_envelopes_in_crime_container_on_weekend() {
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
        envelopesFinder.checkNewCrimeEnvelopesCreated();

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
