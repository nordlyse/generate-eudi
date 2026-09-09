package eu.nordlyse.eudi.crypto;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
import java.util.Locale;

/**
 * PID_03 portrait handling: JPEG bytes for mdoc {@code bstr}, data-URL JPEG for SD-JWT {@code picture}.
 * Opt-out is encoded as an empty byte string.
 */
public final class PortraitProcessor {

    private PortraitProcessor() {
    }

    public static byte[] emptyPortrait() {
        return new byte[0];
    }

    public static byte[] parse(String portraitDataUrl, boolean optOut) {
        if (optOut) {
            return emptyPortrait();
        }
        if (portraitDataUrl == null || portraitDataUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "portrait is required unless the user opts out");
        }
        byte[] bytes = decode(portraitDataUrl.trim());
        if (!isJpeg(bytes)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "portrait must be a JPEG (ISO/IEC 39794-5 / 19794-5 image data), not PNG or another format");
        }
        return bytes;
    }

    public static boolean isJpeg(byte[] bytes) {
        return bytes != null
                && bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    public static String toSdJwtPicture(byte[] jpeg) {
        if (jpeg == null || jpeg.length == 0) {
            return null;
        }
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(jpeg);
    }

    public static String toRawBase64(byte[] jpeg) {
        if (jpeg == null) {
            return "";
        }
        return Base64.getEncoder().encodeToString(jpeg);
    }

    private static byte[] decode(String input) {
        String payload = input;
        if (input.toLowerCase(Locale.ROOT).startsWith("data:")) {
            int comma = input.indexOf(',');
            if (comma < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "portrait data URL is malformed");
            }
            String header = input.substring(0, comma).toLowerCase(Locale.ROOT);
            if (header.contains("image/png")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "portrait must be a JPEG");
            }
            payload = input.substring(comma + 1);
        }
        try {
            return Base64.getDecoder().decode(payload);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "portrait is not valid Base64");
        }
    }
}
