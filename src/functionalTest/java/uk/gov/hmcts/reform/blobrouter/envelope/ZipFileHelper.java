package uk.gov.hmcts.reform.blobrouter.envelope;

import com.google.common.io.Resources;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.google.common.io.Resources.getResource;

public class ZipFileHelper {

    private ZipFileHelper() {
        // utility class
    }

    public static byte[] createZipArchive(List<String> resourceFilePaths) throws IOException, URISyntaxException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            for (String filePath : resourceFilePaths) {
                URL fileUrl = getResource(filePath);
                String fileName = new File(fileUrl.toURI()).getName();
                zos.putNextEntry(new ZipEntry(fileName));
                zos.write(Resources.toByteArray(fileUrl));
                zos.closeEntry();
            }
        }
        return outputStream.toByteArray();
    }
}
