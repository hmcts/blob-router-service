package uk.gov.hmcts.reform.blobrouter.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class NewEnvelopesFinderTest {

    @Mock
    private EnvelopeService envelopeService;

    @Mock
    private ServiceConfiguration serviceConfiguration;

    private NewEnvelopesFinder envelopesFinder;

    @BeforeEach
    public void setup() {
        envelopesFinder = new NewEnvelopesFinder(envelopeService, serviceConfiguration);
    }

    @Test
    void should_check_new_envelopes_in_cft_containers() {
        // given
        List<String> containers = Arrays.asList("c1", "c2", "c3", "crime", "xyz");
        Set<String> cftContainers = Set.of("c1", "c2", "c3", "xyz");

        given(serviceConfiguration.getEnabledSourceContainers())
            .willReturn(containers);

        given(envelopeService.getEnvelopesCount(
            eq(cftContainers), any(), any()
        )).willReturn(1);

        // when
        envelopesFinder.checkNewCftEnvelopesCreated();

        // then
        verify(serviceConfiguration).getEnabledSourceContainers();
        verify(envelopeService).getEnvelopesCount(eq(cftContainers), any(), any());
        verifyNoMoreInteractions(envelopeService);
    }

    @Test
    void should_check_new_envelopes_in_crime_container() {
        // given
        given(envelopeService.getEnvelopesCount(
            eq(singleton("crime")), any(), any()
        )).willReturn(1);

        // when
        envelopesFinder.checkNewCrimeEnvelopesCreated();

        // then
        verifyNoInteractions(serviceConfiguration);
        verify(envelopeService).getEnvelopesCount(eq(singleton("crime")), any(), any());
        verifyNoMoreInteractions(envelopeService);
    }

}
