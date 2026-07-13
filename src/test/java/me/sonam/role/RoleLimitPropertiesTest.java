package me.sonam.role;

import me.sonam.role.config.RoleLimitProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleLimitPropertiesTest {

    @Test
    void returnsHostSpecificLimitForIssuerUrl() {
        RoleLimitProperties properties = new RoleLimitProperties();
        properties.setDefaultMaxRoles(5);
        properties.getHosts().put("demo.openissuer.com", 2);
        properties.getHosts().put("free.openissuer.com", 2);
        properties.getHosts().put("demo.openissuer.test", 2);
        properties.getHosts().put("free.openissuer.test", 2);

        assertThat(properties.maxRolesForIssuer("https://demo.openissuer.com")).isEqualTo(2);
        assertThat(properties.maxRolesForIssuer("https://free.openissuer.com/issuer")).isEqualTo(2);
        assertThat(properties.maxRolesForIssuer("https://demo.openissuer.test")).isEqualTo(2);
        assertThat(properties.maxRolesForIssuer("https://free.openissuer.test/issuer")).isEqualTo(2);
    }

    @Test
    void returnsDefaultLimitWhenIssuerHasNoOverride() {
        RoleLimitProperties properties = new RoleLimitProperties();
        properties.setDefaultMaxRoles(5);
        properties.getHosts().put("demo.openissuer.com", 2);

        assertThat(properties.maxRolesForIssuer("https://business1.openissuer.com")).isEqualTo(5);
        assertThat(properties.maxRolesForIssuer("")).isEqualTo(5);
    }
}
