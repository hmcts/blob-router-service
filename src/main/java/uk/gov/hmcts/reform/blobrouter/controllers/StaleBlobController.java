package uk.gov.hmcts.reform.blobrouter.controllers;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.blobrouter.model.out.BlobInfo;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobLister;

import java.util.List;

@RestController
@RequestMapping(path = "/stale-blobs", produces = MediaType.APPLICATION_JSON_VALUE)
public class StaleBlobController {

    private final BlobLister blobLister;

    private static final String DEFAULT_STALE_TIME = "2";

    public StaleBlobController(BlobLister blobLister) {
        this.blobLister = blobLister;
    }

    @GetMapping
    public List<BlobInfo> findBlobs(
        @RequestParam(name = "stale_time", required = false, defaultValue = DEFAULT_STALE_TIME)
            Integer staleTime
    ) {
        return blobLister.listBlobs(blobLister.timeFilter(staleTime));
    }
}
