package uk.gov.hmcts.reform.blobrouter.testutils;

import org.junit.jupiter.api.Assertions;

import java.io.IOException;

public final class ResourceFilesHelper {

    private static final ClassLoader CLASS_LOADER = ResourceFilesHelper.class.getClassLoader();

    private ResourceFilesHelper() {
        // utility class construct
    }

    public static byte[] getFileContents(String resource) {
        try {
            return CLASS_LOADER.getResourceAsStream(resource).readAllBytes();
        } catch (IOException exception) {
            Assertions.fail("Failed to open resource file " + resource, exception);
            return new byte[]{};
        }
    }
}
