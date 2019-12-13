package uk.gov.hmcts.reform.blobrouter.config;

import com.azure.storage.common.StorageSharedKeyCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfiguration {

    @Bean
    public StorageSharedKeyCredential getStorageSharedKeyCredential(
        @Value("${storage.account_name}") String accountName,
        @Value("${storage.account_key}") String accountKey
    ) {
        System.out.println("Account name " + accountName + " Account key " + accountKey);
        return new StorageSharedKeyCredential(accountName, accountKey);
    }
}
