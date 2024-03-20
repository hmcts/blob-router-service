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

/**
 * The `ZipFileHelper` class provides a method to create a zip archive from a list of file paths and return it as a byte
 * array.
 */
public final class ZipFileHelper {

    private ZipFileHelper() {
        // utility class
    }

    /**
     * The function `createZipArchive` takes a list of file paths, creates a zip archive containing those files, and
     * returns the zip archive as a byte array.
     *
     * @param resourceFilePaths A list of file paths to the resources that you want to include in the zip archive.
     * @return The method `createZipArchive` returns a byte array that represents a zip archive containing the files
     *      specified by the list of resource file paths provided as input.
     */
    public static byte[] createZipArchive(List<String> resourceFilePaths) throws IOException, URISyntaxException {
        var outputStream = new ByteArrayOutputStream();
        try (var zos = new ZipOutputStream(outputStream)) {
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
