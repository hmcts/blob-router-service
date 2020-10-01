package uk.gov.hmcts.reform.blobrouter.services.email;

import java.io.File;
import java.util.Map;

public interface MessageSender {

    void sendMessageWithAttachments(
        String subject,
        String body,
        String from,
        String[] recipients,
        Map<String, File> attachments
    ) throws SendEmailException;

}
