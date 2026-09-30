package ams.asmtravel.api.services.admin.domain;

import java.util.Map;

/** Publishes the seed photos and returns their object keys by file name; rows store keys, not URLs. */
public interface SeedImages {

    /** Publishes the seed photographs and answers each one's object key, by file name. */
    Map<String, String> publish();
}
