package uk.gov.hmcts.reform.bulkscanning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
public class BulkScanningApiApplication {
    private static final Logger LOG = LoggerFactory.getLogger(BulkScanningApiApplication.class);

    public static void main(final String[] args) {
        try{
            SpringApplication.run(BulkScanningApiApplication.class, args);
        }catch (RuntimeException re) {
            LOG.error("Application crashed with error message: ", re);
        }

    }
}
