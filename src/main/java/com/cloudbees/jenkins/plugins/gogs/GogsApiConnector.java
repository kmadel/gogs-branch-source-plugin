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
package com.cloudbees.jenkins.plugins.gogs;

import java.util.List;

import javax.annotation.CheckForNull;

import com.cloudbees.jenkins.plugins.gogs.api.GogsApi;
import com.cloudbees.jenkins.plugins.gogs.server.client.GogsServerAPIClient;
import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import hudson.Util;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import jenkins.scm.api.SCMSourceOwner;

public class GogsApiConnector {

    private String serverUrl;

    public GogsApiConnector() {
    }

    public GogsApiConnector(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public GogsApi create(String owner, String repository, StandardUsernamePasswordCredentials creds) {
        return new GogsServerAPIClient(serverUrl, owner, repository, creds, false);
    }

    public GogsApi create(String owner, StandardUsernamePasswordCredentials creds) {
        return new GogsServerAPIClient(serverUrl, owner, creds, false);
    }

    @CheckForNull 
    public <T extends StandardCredentials> T lookupCredentials(@CheckForNull SCMSourceOwner context, @CheckForNull String id, Class<T> type) {
        if (Util.fixEmpty(id) == null) {
            return null;
        } else {
            if (id != null) {
                return CredentialsMatchers.firstOrNull(
                          CredentialsProvider.lookupCredentials(type, context, ACL.SYSTEM,
                          gogsDomainRequirements()),
                          CredentialsMatchers.allOf(
                              CredentialsMatchers.withId(id),
                              CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(type))));
            }
            return null;
        }
    }

    public ListBoxModel fillCheckoutCredentials(StandardListBoxModel result, SCMSourceOwner context) {
        result.withMatching(gogsCheckoutCredentialsMatcher(), CredentialsProvider.lookupCredentials(
                StandardCredentials.class, context, ACL.SYSTEM, gogsDomainRequirements()));
        return result;
    }

    public ListBoxModel fillCredentials(StandardListBoxModel result, SCMSourceOwner context) {
        result.withMatching(gogsCredentialsMatcher(), CredentialsProvider.lookupCredentials(
                StandardUsernameCredentials.class, context, ACL.SYSTEM, gogsDomainRequirements()));
        return result;
    }

    /* package */ static CredentialsMatcher gogsCredentialsMatcher() {
        return CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class));
    }

    /* package */ static CredentialsMatcher gogsCheckoutCredentialsMatcher() {
        return CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardCredentials.class));
    }

    /* package */ List<DomainRequirement> gogsDomainRequirements() {
        return URIRequirementBuilder.fromUri(serverUrl).build();
    }

}
