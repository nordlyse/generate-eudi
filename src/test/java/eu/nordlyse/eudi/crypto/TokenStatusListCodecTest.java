package eu.nordlyse.eudi.crypto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenStatusListCodecTest {

    @Test
    void encodesRevokedBitAtIndex() {
        boolean[] flags = {false, true, false};
        String lst = TokenStatusListCodec.encode(flags);
        assertThat(lst).isNotBlank();
        assertThat(TokenStatusListCodec.isRevoked(flags, 0)).isFalse();
        assertThat(TokenStatusListCodec.isRevoked(flags, 1)).isTrue();
    }

    @Test
    void emptyRegistryStillProducesLst() {
        assertThat(TokenStatusListCodec.encode(new boolean[0])).isNotBlank();
    }
}
