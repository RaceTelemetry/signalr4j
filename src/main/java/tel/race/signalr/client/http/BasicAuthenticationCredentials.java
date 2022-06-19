/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package tel.race.signalr.client.http;

import tel.race.signalr.client.Credentials;

/**
 * Credentials implementation for HTTP Basic Authentication
 */
public class BasicAuthenticationCredentials implements Credentials {
    private String username;
    private String password;
    private Base64Encoder encoder;

    /**
     * Creates a BasicAuthenticationCredentials instance with a username,
     * password and an encoder
     *
     * @param username The username for the credentials
     * @param password The password for the credentials
     * @param encoder  The Base64 encoder to use
     */
    public BasicAuthenticationCredentials(String username, String password, Base64Encoder encoder) {
        initialize(username, password, encoder);
    }

    /**
     * Initializes a BasicAuthenticationCredentials instance with a username and
     * a password
     *
     * @param username The username for the credentials
     * @param password The password for the credentials
     * @param encoder  The Base64 encoder to use
     */
    private void initialize(String username, String password, Base64Encoder encoder) {
        this.username = username;
        this.password = password;
        this.encoder = encoder;

        if (encoder == null) {
            throw new IllegalArgumentException("encoder");
        }
    }

    /**
     * Returns the username for the credentials
     *
     * @return username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username for the credentials
     *
     * @param username username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the password for the credentials
     *
     * @return password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password for the credentials
     *
     * @param password password for the credentials
     */
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public void prepareRequest(Request request) {
        String headerValue = username + ":" + password;

        headerValue = encoder.encodeBytes(headerValue.getBytes()).trim();

        request.addHeader("Authorization", "Basic " + headerValue);
    }

    /**
     * Represents a Base64Encoder
     */
    public interface Base64Encoder {
        /**
         * Encodes a byte array
         *
         * @param bytes Bytes to encode
         * @return The encoded bytes
         */
        public String encodeBytes(byte[] bytes);
    }

    public class InvalidPlatformException extends Exception {
        private static final long serialVersionUID = 1975952258601813204L;
    }
}
