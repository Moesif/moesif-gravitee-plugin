package com.moesif.moesifgraviteeplugin.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URL;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MoesifPolicyConfigurationTest {

    @Test
    public void test_transformHeaders01() throws IOException {
        MoesifPolicyConfiguration configuration = load(
                "/com/moesif/moesifgraviteeplugin/configuration/transformheaders01.json",
            MoesifPolicyConfiguration.class
        );

        assertThat(configuration.getScope()).isEqualTo(PolicyScope.REQUEST);
        assertThat(configuration.getAddHeaders()).isNotNull();
        assertThat(configuration.getRemoveHeaders()).isNotNull();
        assertThat(configuration.getWhitelistHeaders()).isNull();

        assertThat(configuration.getAddHeaders()).hasSize(1);
        assertThat(configuration.getRemoveHeaders()).hasSize(1);
    }

    @Test
    public void test_transformHeaders02() throws IOException {
        MoesifPolicyConfiguration configuration = load(
                "/com/moesif/moesifgraviteeplugin/configuration/transformheaders02.json",
            MoesifPolicyConfiguration.class
        );

        assertThat(configuration.getScope()).isEqualTo(PolicyScope.RESPONSE);
        assertThat(configuration.getAddHeaders()).isNotNull();
        assertThat(configuration.getRemoveHeaders()).isNotNull();
        assertThat(configuration.getWhitelistHeaders()).isNull();

        assertThat(configuration.getAddHeaders()).hasSize(1);
        assertThat(configuration.getRemoveHeaders()).hasSize(1);
    }

    @Test
    public void test_transformHeaders03() throws IOException {
        MoesifPolicyConfiguration configuration = load(
                "/com/moesif/moesifgraviteeplugin/configuration/transformheaders03.json",
            MoesifPolicyConfiguration.class
        );

        assertThat(configuration.getScope()).isEqualTo(PolicyScope.RESPONSE);
        assertThat(configuration.getAddHeaders()).isNotNull();
        assertThat(configuration.getRemoveHeaders()).isNotNull();
        assertThat(configuration.getWhitelistHeaders()).isNull();

        assertThat(configuration.getAddHeaders()).hasSize(2);
        assertThat(configuration.getRemoveHeaders()).hasSize(1);
    }

    @Test
    public void test_transformHeaders04() throws IOException {
        MoesifPolicyConfiguration configuration = load(
                "/com/moesif/moesifgraviteeplugin/configuration/transformheaders04.json",
            MoesifPolicyConfiguration.class
        );

        assertThat(configuration.getScope()).isEqualTo(PolicyScope.RESPONSE);
        assertThat(configuration.getAddHeaders()).isNull();
        assertThat(configuration.getRemoveHeaders()).isNotNull();
        assertThat(configuration.getWhitelistHeaders()).isNotNull();

        assertThat(configuration.getRemoveHeaders()).hasSize(1);
        assertThat(configuration.getWhitelistHeaders()).hasSize(2);
    }

    private <T> T load(String resource, Class<T> type) throws IOException {
        URL jsonFile = this.getClass().getResource(resource);
        return new ObjectMapper().readValue(jsonFile, type);
    }
}
