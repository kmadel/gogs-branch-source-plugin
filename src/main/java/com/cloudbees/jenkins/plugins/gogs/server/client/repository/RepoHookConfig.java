package com.cloudbees.jenkins.plugins.gogs.server.client.repository;

import com.cloudbees.jenkins.plugins.gogs.api.GogsWebhookConfig;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RepoHookConfig implements GogsWebhookConfig{

    private String url;

    private String content_type;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getContent_type() {
        return content_type;
    }

    public void setContent_type(String content_type) {
        this.content_type = content_type;
    }
}
