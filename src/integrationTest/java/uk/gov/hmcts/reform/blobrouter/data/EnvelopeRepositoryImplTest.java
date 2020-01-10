package uk.gov.hmcts.reform.blobrouter.data;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.blobrouter.data.model.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.model.Status;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class EnvelopeRepositoryImplTest {

    @Autowired
    private EnvelopeRepositoryImpl repo;

    @Test
    public void test() {
        List<Envelope> envelopes = repo.find(Status.REJECTED, false);
        assertThat(true).isFalse();
    }
}
