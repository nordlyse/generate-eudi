package eu.nordlyse.eudi.crypto;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PortraitProcessorTest {

    public static final byte[] TINY_JPEG = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xD9};
    public static final byte[] PNG_HEADER = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x00
    };

    @Test
    void optOutIsEmptyByteString() {
        byte[] empty = PortraitProcessor.parse(null, true);
        assertThat(empty).isEmpty();
        assertThat(PortraitProcessor.toSdJwtPicture(empty)).isNull();
        assertThat(PortraitProcessor.toRawBase64(empty)).isEmpty();
    }

    @Test
    void jpegDataUrlIsAccepted() {
        String dataUrl = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(TINY_JPEG);
        byte[] parsed = PortraitProcessor.parse(dataUrl, false);
        assertThat(parsed).isEqualTo(TINY_JPEG);
        assertThat(PortraitProcessor.toSdJwtPicture(parsed)).startsWith("data:image/jpeg;base64,");
        assertThat(PortraitProcessor.toRawBase64(parsed)).doesNotContain("data:");
    }

    @Test
    void pngIsRejected() {
        String dataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(PNG_HEADER);
        assertThatThrownBy(() -> PortraitProcessor.parse(dataUrl, false))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("JPEG");
    }
}
