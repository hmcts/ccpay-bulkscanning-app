package uk.gov.hmcts.reform.bulkscanning.payments;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
//@ActiveProfiles({"local"})
@SpringBootTest
@TestPropertySource(locations="classpath:application-test.properties")

public class ScannedChequeTester {


    @Test
    public void welcomeRootEndpoint() {

        assertThat("Welcome to world of scanning").startsWith("Welcome");
    }
}
