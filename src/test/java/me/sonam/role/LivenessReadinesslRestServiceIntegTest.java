package me.sonam.role;


import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Test the liveness and readiness endpoints
 */
@AutoConfigureWebTestClient
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SpringApplication.class})
public class LivenessReadinesslRestServiceIntegTest {
  private static final Logger LOG = LoggerFactory.getLogger(LivenessReadinesslRestServiceIntegTest.class);

  @MockBean
  private ReactiveJwtDecoder reactiveJwtDecoder;

  @Autowired
  private WebTestClient client;

  @Test
  public void readiness() {
    LOG.info("check readiness endpoint");
    client.get().uri("/roles/api/health/readiness")
            .exchange().expectStatus().isOk();
  }

  @Test
  public void liveness() {
    LOG.info("check liveness endpoint");
    client.get().uri("/roles/api/health/liveness")
            .exchange().expectStatus().isOk();
  }
}
