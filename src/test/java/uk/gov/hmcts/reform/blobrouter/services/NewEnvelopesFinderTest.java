package uk.gov.hmcts.reform.blobrouter.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class NewEnvelopesFinderTest {

    @Mock
    private EnvelopeRepository envelopeRepository;

    @Mock
    private ServiceConfiguration serviceConfiguration;

    private NewEnvelopesFinder envelopesFinder;

    @BeforeEach
    public void setup() {
        envelopesFinder = new NewEnvelopesFinder(envelopeRepository, serviceConfiguration);
    }

    @Test
    void should_check_new_envelopes_in_cft_containers() {
        // given
        List<String> containers = Arrays.asList("c1", "c2", "c3", "crime", "xyz");
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
    void should_check_new_envelopes_in_crime_container() {
        // given
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

}
