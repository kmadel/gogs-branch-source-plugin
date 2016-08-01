package com.cloudbees.jenkins.plugins.gogs.server.client.repository;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * Created by kmadel on 8/1/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayloadOwner {

    private String name;
    private String email;
    private String username;

    public String getName() {
        return name;
    }

    public PayloadOwner setName(String name) {
        this.name = name;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public PayloadOwner setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public PayloadOwner setUsername(String username) {
        this.username = username;
        return this;
    }
}
