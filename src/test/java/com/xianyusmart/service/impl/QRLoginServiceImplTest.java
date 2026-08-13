package com.xianyusmart.service.impl;

import com.google.gson.JsonObject;
import com.xianyusmart.controller.dto.QRLoginSession;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QRLoginServiceImplTest {

    @Test
    void mergesPassportCookiesAcrossQrRequests() {
        QRLoginSession session = new QRLoginSession("session-1");

        QRLoginServiceImpl.mergeSetCookieHeaders(session, List.of(
                "loginKey=abc; Path=/; HttpOnly",
                "unb=22129504; Domain=.goofish.com; Path=/"));

        assertEquals("abc", session.getCookies().get("loginKey"));
        assertEquals("22129504", session.getUnb());
    }

    @Test
    void acceptsCurrentAndLegacyScannedStatusNames() {
        assertTrue(QRLoginServiceImpl.isScannedStatus("SCANED"));
        assertTrue(QRLoginServiceImpl.isScannedStatus("SCANNED"));
    }

    @Test
    void readsStatusFromCompatibleResponseFields() {
        JsonObject data = new JsonObject();
        data.addProperty("status", "confirmed");

        assertEquals("CONFIRMED", QRLoginServiceImpl.readQRCodeStatus(data));
    }

    @Test
    void treatsCancellationOnlyAsAnExplicitTerminalStatus() {
        assertTrue(QRLoginServiceImpl.isCancelledStatus("CANCELLED"));
        assertTrue(!QRLoginServiceImpl.isCancelledStatus("PROCESSING"));
    }

    @Test
    void verificationSessionCanBeExtendedForHumanCheck() {
        QRLoginSession session = new QRLoginSession("session-2");

        session.setExpireTime(900000);

        assertEquals(900000, session.getExpireTime());
        assertTrue(!session.isExpired());
        assertTrue(!QRLoginServiceImpl.isTerminalSessionStatus("verification_required"));
        assertTrue(QRLoginServiceImpl.isTerminalSessionStatus("success"));
    }

    @Test
    void keepsWaitingWhenConsumedQrBelongsToVerificationSession() {
        assertTrue(QRLoginServiceImpl.shouldKeepWaitingAfterQRCodeExpired("verification_required"));
        assertTrue(!QRLoginServiceImpl.shouldKeepWaitingAfterQRCodeExpired("waiting"));
        assertTrue(!QRLoginServiceImpl.shouldKeepWaitingAfterQRCodeExpired("scanned"));
    }

    @Test
    void verificationWindowCanRestartAfterOriginalQrLifetime() {
        QRLoginSession session = new QRLoginSession("session-3");
        session.setCreatedTime(System.currentTimeMillis() - 20 * 60 * 1000L);
        assertTrue(session.isExpired());

        session.setCreatedTime(System.currentTimeMillis());
        session.setExpireTime(15 * 60 * 1000L);

        assertTrue(!session.isExpired());
    }

    @Test
    void extractsFaceVerificationProtocolValues() {
        String normalHtml = """
                <script>
                window.location.href="https:\\/\\/passport.goofish.com\\/iv\\/mini\\/verify_modes.htm?htoken=token_1&amp;_umidfg=";
                </script>
                """;
        HttpUrl pageUrl = HttpUrl.get("https://passport.goofish.com/iv/mini/normal_validate.htm?htoken=token_1");

        assertEquals("token_1", QRLoginServiceImpl.extractFaceToken(normalHtml, pageUrl.toString()));
        assertEquals(
                "https://passport.goofish.com/iv/mini/verify_modes.htm?htoken=token_1&_umidfg=1",
                QRLoginServiceImpl.extractVerificationModesUrl(normalHtml, pageUrl));
        assertEquals(
                "https://passport.goofish.com/iv/face?foo=1&bar=2",
                QRLoginServiceImpl.extractFaceQRCodeContent(
                        "new Qrcode({ text: \"https:\\/\\/passport.goofish.com\\/iv\\/face?foo=1&amp;bar=2\" });"));
    }

    @Test
    void readsSuccessfulFaceVerificationCallback() {
        JsonObject content = new JsonObject();
        content.addProperty("code", 3);
        content.addProperty("url", "https://passport.goofish.com/iv/mini/ivCheckLogin.htm?token=done");
        JsonObject response = new JsonObject();
        response.add("content", content);

        QRLoginServiceImpl.FaceCheckResult result = QRLoginServiceImpl.readFaceCheckResult(response);

        assertEquals("3", result.code());
        assertEquals("https://passport.goofish.com/iv/mini/ivCheckLogin.htm?token=done", result.completionUrl());
    }

    @Test
    void onlyAllowsTrustedVerificationHosts() {
        assertTrue(QRLoginServiceImpl.isTrustedVerificationHost("passport.goofish.com"));
        assertTrue(QRLoginServiceImpl.isTrustedVerificationHost("login.taobao.com"));
        assertTrue(!QRLoginServiceImpl.isTrustedVerificationHost("goofish.com.attacker.example"));
    }
}
