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

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.jenkins.plugins.gogs.api.GogsApi;
import com.cloudbees.jenkins.plugins.gogs.api.GogsRepository;
import com.cloudbees.jenkins.plugins.gogs.api.GogsOrganization;
import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMNavigatorDescriptor;
import jenkins.scm.api.SCMSourceObserver;
import jenkins.scm.api.SCMSourceOwner;

public class GogsSCMNavigator extends SCMNavigator {

    private final String repoOwner;
    private final String credentialsId;
    private final String checkoutCredentialsId;
    private String pattern = ".*";
    private boolean autoRegisterHooks = false;
    private String gogsServerUrl;
    private int sshPort = -1;

    /**
     * Gogs API client connector.
     */
    private transient GogsApiConnector gogsConnector;

    @DataBoundConstructor 
    public GogsSCMNavigator(String repoOwner, String credentialsId, String checkoutCredentialsId) {
        this.repoOwner = repoOwner;
        this.credentialsId = Util.fixEmpty(credentialsId);
        this.checkoutCredentialsId = checkoutCredentialsId;
    }

    @DataBoundSetter 
    public void setPattern(String pattern) {
        Pattern.compile(pattern);
        this.pattern = pattern;
    }

    @DataBoundSetter
    public void setAutoRegisterHooks(boolean autoRegisterHooks) {
        this.autoRegisterHooks = autoRegisterHooks;
    }

    public String getRepoOwner() {
        return repoOwner;
    }

    @CheckForNull
    public String getCredentialsId() {
        return credentialsId;
    }

    @CheckForNull
    public String getCheckoutCredentialsId() {
        return checkoutCredentialsId;
    }

    public String getPattern() {
        return pattern;
    }

    public boolean isAutoRegisterHooks() {
        return autoRegisterHooks;
    }

    public int getSshPort() {
        return sshPort;
    }

    @DataBoundSetter
    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }

    @DataBoundSetter
    public void setGogsServerUrl(String url) {
        this.gogsServerUrl = Util.fixEmpty(url);
        if (this.gogsServerUrl != null) {
            // Remove a possible trailing slash
            this.gogsServerUrl = this.gogsServerUrl.replaceAll("/$", "");
        }
    }

    @CheckForNull
    public String getGogsServerUrl() {
        return gogsServerUrl;
    }

    public void setGogsConnector(@NonNull GogsApiConnector gogsConnector) {
        this.gogsConnector = gogsConnector;
    }

    private GogsApiConnector getGogsConnector() {
        if (gogsConnector == null) {
            gogsConnector = new GogsApiConnector(gogsServerUrl);
        }
        return gogsConnector;
    }

    @NonNull
    @Override
    protected String id() {
        return getGogsServerUrl() + "::" + repoOwner ;
    }

    @Override
    public void visitSources(SCMSourceObserver observer) throws IOException, InterruptedException {
        TaskListener listener = observer.getListener();

        if (StringUtils.isBlank(repoOwner)) {
            listener.getLogger().format("Must specify a repository owner%n");
            return;
        }
        StandardUsernamePasswordCredentials credentials = getGogsConnector().lookupCredentials(observer.getContext(),
                credentialsId, StandardUsernamePasswordCredentials.class);

        if (credentials == null) {
            listener.getLogger().format("Connecting to %s with no credentials, anonymous access%n", gogsServerUrl == null ? "https://gogs.org" : gogsServerUrl);
        } else {
            listener.getLogger().format("Connecting to %s using %s%n", gogsServerUrl == null ? "https://gogs.org" : gogsServerUrl, CredentialsNameProvider.name(credentials));
        }
        List<? extends GogsRepository> repositories;
        GogsApi gogs = getGogsConnector().create(repoOwner, credentials);
        GogsOrganization organization = gogs.getOrganization();
        if (organization != null) {
            // Navigate repositories of the team
            listener.getLogger().format("Looking up repositories of organization %s%n", repoOwner);
            repositories = gogs.getRepositories();
        } else {
            // Navigate the repositories of the repoOwner as a user
            listener.getLogger().format("Looking up repositories of user %s%n", repoOwner);
            repositories = gogs.getRepositories();
        }
        for (GogsRepository repo : repositories) {
            add(listener, observer, repo);
        }
    }

    private void add(TaskListener listener, SCMSourceObserver observer, GogsRepository repo) throws InterruptedException {
        String name = repo.getRepositoryName();
        if (!Pattern.compile(pattern).matcher(name).matches()) {
            listener.getLogger().format("Ignoring %s%n", name);
            return;
        }
        listener.getLogger().format("Proposing %s%n", name);
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        SCMSourceObserver.ProjectObserver projectObserver = observer.observe(name);
        GogsSCMSource scmSource = new GogsSCMSource(null, repoOwner, name);
        scmSource.setGogsConnector(getGogsConnector());
        scmSource.setCredentialsId(credentialsId);
        scmSource.setCheckoutCredentialsId(checkoutCredentialsId);
        scmSource.setAutoRegisterHook(isAutoRegisterHooks());
        scmSource.setGogsServerUrl(gogsServerUrl);
        scmSource.setSshPort(sshPort);
        projectObserver.addSource(scmSource);
        projectObserver.complete();
    }

    @Extension 
    public static class DescriptorImpl extends SCMNavigatorDescriptor {

        public static final String ANONYMOUS = GogsSCMSource.DescriptorImpl.ANONYMOUS;
        public static final String SAME = GogsSCMSource.DescriptorImpl.SAME;

        @Override
        public String getDisplayName() {
            return Messages.GogsSCMNavigator_DisplayName();
        }

        @Override
        public String getDescription() {
            return Messages.GogsSCMNavigator_Description();
        }

        @Override
        public String getIconFilePathPattern() {
            return "plugin/gogs-branch-source/images/:size/gogs-scmnavigator.png";
        }

        @Override
        public SCMNavigator newInstance(String name) {
            return new GogsSCMNavigator(name, "", GogsSCMSource.DescriptorImpl.SAME);
        }

        public FormValidation doCheckCredentialsId(@QueryParameter String value) {
            if (!value.isEmpty()) {
                return FormValidation.ok();
            } else {
                return FormValidation.warning("Credentials are required for build notifications");
            }
        }

        public FormValidation doCheckGogsServerUrl(@QueryParameter String gogsServerUrl) {
            return GogsSCMSource.DescriptorImpl.doCheckGogsServerUrl(gogsServerUrl);
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath SCMSourceOwner context, @QueryParameter String gogsServerUrl) {
            StandardListBoxModel result = new StandardListBoxModel();
            result.withEmptySelection();
            new GogsApiConnector(gogsServerUrl).fillCredentials(result, context);
            return result;
        }

        public ListBoxModel doFillCheckoutCredentialsIdItems(@AncestorInPath SCMSourceOwner context, @QueryParameter String gogsServerUrl) {
            StandardListBoxModel result = new StandardListBoxModel();
            result.add("- same as scan credentials -", GogsSCMSource.DescriptorImpl.SAME);
            result.add("- anonymous -", GogsSCMSource.DescriptorImpl.ANONYMOUS);
            new GogsApiConnector(gogsServerUrl).fillCheckoutCredentials(result, context);
            return result;
        }

    }
}
