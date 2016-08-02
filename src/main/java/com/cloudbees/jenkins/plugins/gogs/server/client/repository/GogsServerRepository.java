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

import com.cloudbees.jenkins.plugins.gogs.api.GogsRepository;
import com.cloudbees.jenkins.plugins.gogs.api.GogsRepositoryOwner;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GogsServerRepository implements GogsRepository {

    @JsonProperty("full_name")
    private String fullName;

    private GogsServerRepositoryOwner owner;

    private String description;

    @JsonProperty("html_url")
    private String htmlUrl;

    // JSON mapping added in setter because the field can not be called "private"
    private Boolean priv;

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public GogsRepositoryOwner getOwner() {
        return owner;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setOwner(GogsServerRepositoryOwner owner) {
        this.owner = owner;
    }

    @Override
    public String getOwnerName() {
        if (this.fullName != null) {
            return this.fullName.split("/")[0];
        }
        return null;
    }

    @Override
    public String getRepositoryName() {
        if (this.fullName != null) {
            return this.fullName.split("/")[1];
        }
        return null;
    }

    @Override
    public String getHtmlUrl() {
        return htmlUrl;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean isPrivate() {
        return priv;
    }

    @JsonProperty("is_private")
    public void setPrivate(Boolean priv) {
        this.priv = priv;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

}
