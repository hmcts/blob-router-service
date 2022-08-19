package uk.gov.hmcts.reform.blobrouter.services;

import com.azure.storage.common.StorageSharedKeyCredential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.config.StorageConfigItem;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.TeamContainerProcessor;

import java.util.HashMap;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

public class TeamEnvelopeServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    StorageSharedKeyCredential storageSharedKeyCredential;

    @Mock
    ServiceConfiguration serviceConfiguration;

    @Mock
    private TeamContainerProcessor teamContainerProcessor;

    TeamEnvelopeService teamEnvelopeService;

    @BeforeEach
    void setUp() {
        serviceConfiguration = Mockito.mock(ServiceConfiguration.class);
        teamContainerProcessor = Mockito.mock(TeamContainerProcessor.class);
        teamEnvelopeService = new TeamEnvelopeService(storageSharedKeyCredential,
                                                      serviceConfiguration,
                                                      teamContainerProcessor);
    }

    @Test
    void should_return_something() {
        given(teamContainerProcessor.leaseAndGetEnvelopes(anyString()))
            .willReturn(emptyList());
        given(serviceConfiguration.getStorageConfig())
            .willReturn(new HashMap<String, StorageConfigItem>() {
                {
                    put("nfd", new StorageConfigItem());
                }
            });

        var teamEnvelopes = teamEnvelopeService.getEnvelopes("nfd");
        assertThat(teamEnvelopes).isEqualTo(emptyList());
    }
}
