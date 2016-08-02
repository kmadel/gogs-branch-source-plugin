/*
 * The MIT License
 *
 * Copyright (c) 2016, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.cloudbees.jenkins.plugins.gogs.server.client.repository;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import com.cloudbees.jenkins.plugins.gogs.api.GogsOrganization;

import java.net.MalformedURLException;
import java.net.URL;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GogsServerOrganization implements GogsOrganization {

    @JsonProperty("username")
    private String name;

    @JsonProperty("full_name")
    private String displayName;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    private String description;

    private URL htmlUrl;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    @Override
    public URL getHtmlUrl() throws MalformedURLException {
        // build Gogs Org URL from avatarUrl
        URL endpoint = new URL(this.avatarUrl);
        String protocol = this.avatarUrl.startsWith("https://") ? "https://" : "http://";
        return new URL(protocol + endpoint.getHost() + ":" + endpoint.getPort() + "/" + this.name);
    }

    public void setHtmlUrl(URL htmlUrl) {
        this.htmlUrl = htmlUrl;
    }
}
