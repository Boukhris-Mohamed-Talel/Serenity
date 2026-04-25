package com.example.appointment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OAuth client for Google Calendar sync. Bind from {@code GOOGLE_CALENDAR_*} env vars
 * (see {@code application.properties}). Optional JSON path is filled by {@link GoogleCalendarCredentialsLoader}.
 */
@Data
@ConfigurationProperties(prefix = "app.google.calendar")
public class GoogleCalendarProperties {

    /**
     * When false, Google Calendar endpoints return 503.
     */
    private boolean enabled = true;

    private String clientId = "";
    private String clientSecret = "";

    /**
     * Absolute path to Google Cloud "Download JSON" OAuth client file (web client).
     */
    private String credentialsJsonPath = "";

    /**
     * Must match an Authorized redirect URI in Google Cloud Console exactly.
     */
    private String redirectUri = "http://localhost:4200/appointments/oauth/calendar";
}
