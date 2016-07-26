package org.dsa.iot.calendar.google;

import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;

import java.io.IOException;

public class DSAPromptReceiver implements VerificationCodeReceiver {
    private String code;

    public DSAPromptReceiver(String code) {
        this.code = code;
    }

    @Override
    public String getRedirectUri() throws IOException {
        return GoogleOAuthConstants.OOB_REDIRECT_URI;
    }

    @Override
    public String waitForCode() throws IOException {
        return code;
    }

    @Override
    public void stop() throws IOException {
    }
}
