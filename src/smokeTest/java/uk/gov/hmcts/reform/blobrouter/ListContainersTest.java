package uk.gov.hmcts.reform.blobrouter;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.blobrouter.util.StorageHelper.STORAGE_CLIENT;

class ListContainersTest {

    @DisplayName("Storage should already have at least one container")
    @Test
    @Disabled("temporarily")
    void should_have_at_least_one_container() {
        assertThat(STORAGE_CLIENT.listBlobContainers().toStream()).isNotEmpty();
    }
}
