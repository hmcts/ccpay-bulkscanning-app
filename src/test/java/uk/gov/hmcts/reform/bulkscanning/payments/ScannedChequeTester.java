package uk.gov.hmcts.reform.bulkscanning.payments;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local"})
@SpringBootTest
public class ScannedChequeTester {


    @Test
    public void welcomeRootEndpoint() {

        assertThat("Welcome to world of scanning").startsWith("Welcome");
    }
}
