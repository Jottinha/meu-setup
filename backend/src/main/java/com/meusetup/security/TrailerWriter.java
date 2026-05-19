package com.meusetup.security;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * Trailer binario anexado ao .exe:
 *   PAYLOAD_JSON   (N bytes, JSON minificado)
 *   HMAC           (32 bytes, HMAC-SHA256 sobre MAGIC || PAYLOAD_LEN || PAYLOAD_JSON)
 *   PAYLOAD_LEN    (4 bytes, uint32 little-endian)
 *   MAGIC          (8 bytes: "MSTUP" + 0x01 + 0x00 + 0x00)  <- ultimos 8 bytes
 */
@Component
public class TrailerWriter {

    public static final byte[] MAGIC = new byte[] {
        'M', 'S', 'T', 'U', 'P', 0x01, 0x00, 0x00
    };
    public static final int HMAC_LEN = 32;
    public static final int LEN_FIELD = 4;
    public static final int TRAILER_FIXED = MAGIC.length + LEN_FIELD + HMAC_LEN; // 44

    private final HmacKeyProvider hmacKey;

    public TrailerWriter(HmacKeyProvider hmacKey) {
        this.hmacKey = hmacKey;
    }

    public byte[] build(List<String> appIds) {
        String json = buildPayloadJson(appIds);
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);

        ByteBuffer lenBuf = ByteBuffer.allocate(LEN_FIELD).order(ByteOrder.LITTLE_ENDIAN);
        lenBuf.putInt(payload.length);
        byte[] lenBytes = lenBuf.array();

        byte[] hmac = computeHmac(payload, lenBytes);

        ByteBuffer out = ByteBuffer.allocate(payload.length + TRAILER_FIXED);
        out.put(payload);
        out.put(hmac);
        out.put(lenBytes);
        out.put(MAGIC);
        return out.array();
    }

    private byte[] computeHmac(byte[] payload, byte[] lenBytes) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey.get(), "HmacSHA256"));
            mac.update(MAGIC);
            mac.update(lenBytes);
            mac.update(payload);
            return mac.doFinal();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 indisponivel", e);
        }
    }

    private static String buildPayloadJson(List<String> appIds) {
        StringBuilder sb = new StringBuilder(64 + appIds.size() * 32);
        sb.append("{\"v\":1,\"apps\":[");
        for (int i = 0; i < appIds.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(appIds.get(i)).append('"');
        }
        sb.append("]}");
        return sb.toString();
    }
}
