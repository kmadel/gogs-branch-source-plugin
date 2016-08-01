package com.cloudbees.jenkins.plugins.gogs.server.client.repository;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Payload repository
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayloadRepo {

    private Long id;
    private String name;
    private String url;
    private String description;
    private String website;
    private Integer watchers;
    private PayloadOwner owner;
    @JsonProperty("private")
    private Boolean private_;

    public Long getId() {
        return id;
    }

    public PayloadRepo setId(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public PayloadRepo setName(String name) {
        this.name = name;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public PayloadRepo setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public PayloadRepo setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getWebsite() {
        return website;
    }

    public PayloadRepo setWebsite(String website) {
        this.website = website;
        return this;
    }

    public Integer getWatchers() {
        return watchers;
    }

    public PayloadRepo setWatchers(Integer watchers) {
        this.watchers = watchers;
        return this;
    }

    public PayloadOwner getOwner() {
        return owner;
    }

    public PayloadRepo setOwner(PayloadOwner owner) {
        this.owner = owner;
        return this;
    }

    public Boolean getPrivate_() {
        return private_;
    }

    public PayloadRepo setPrivate_(Boolean private_) {
        this.private_ = private_;
        return this;
    }
}
