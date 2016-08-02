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

import com.cloudbees.jenkins.plugins.gogs.api.GogsApi;
import com.cloudbees.jenkins.plugins.gogs.api.GogsBranch;
import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.scm.SCM;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.*;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.RefSpec;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * SCM source implementation for Gogs.
 * 
 * It provides a way to discover/retrieve branches and pull requests through the Gogs REST API
 * which is much faster than the plain Git SCM source implementation.
 */
public class GogsSCMSource extends AbstractGitSCMSource {

    /**
     * Credentials used to access the Gogs REST API.
     */
    private String credentialsId;

    /**
     * Credentials used to clone the repository/repositories.
     */
    private String checkoutCredentialsId;

    /**
     * Repository owner.
     * Used to build the repository URL.
     */
    private final String repoOwner;

    /**
     * Repository name.
     * Used to build the repository URL.
     */
    private final String repository;

    /**
     * Ant match expression that indicates what branches to include in the retrieve process.
     */
    private String includes = "*";

    /**
     * Ant match expression that indicates what branches to exclude in the retrieve process.
     */
    private String excludes = "";

    /**
     * If true, a webhook will be auto-registered in the repository managed by this source.
     */
    private boolean autoRegisterHook = false;

    /**
     * Gogs Server URL.
     * An specific HTTP client is used if this field is not null.
     */
    private String gogsServerUrl;

    /**
     * Port used by Gogs Server for SSH clone.
     */
    private int sshPort = -1;

    /**
     * Gogs API client connector.
     */
    private transient GogsApiConnector gogsConnector;

    private static final Logger LOGGER = Logger.getLogger(GogsSCMSource.class.getName());

    @DataBoundConstructor
    public GogsSCMSource(String id, String repoOwner, String repository) {
        super(id);
        this.repoOwner = repoOwner;
        this.repository = repository;
    }

    @CheckForNull
    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = Util.fixEmpty(credentialsId);
    }

    @CheckForNull
    public String getCheckoutCredentialsId() {
        return checkoutCredentialsId;
    }

    @DataBoundSetter
    public void setCheckoutCredentialsId(String checkoutCredentialsId) {
        this.checkoutCredentialsId = checkoutCredentialsId;
    }

    public String getIncludes() {
        return includes;
    }

    @DataBoundSetter
    public void setIncludes(@NonNull String includes) {
        Pattern.compile(getPattern(includes));
        this.includes = includes;
    }

    public String getExcludes() {
        return excludes;
    }

    @DataBoundSetter
    public void setExcludes(@NonNull String excludes) {
        Pattern.compile(getPattern(excludes));
        this.excludes = excludes;
    }

    public String getRepoOwner() {
        return repoOwner;
    }

    public String getRepository() {
        return repository;
    }

    @DataBoundSetter
    public void setAutoRegisterHook(boolean autoRegisterHook) {
        this.autoRegisterHook = autoRegisterHook;
    }

    public boolean isAutoRegisterHook() {
        return autoRegisterHook;
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

    @Override
    public String getRemote() {
        return getUriResolver().getRepositoryUri(gogsServerUrl, repoOwner, repository);
    }


    public GogsApi buildGogsClient() {
        return getGogsConnector().create(repoOwner, repository, getScanCredentials());
    }

    @Override
    protected void retrieve(SCMHeadObserver observer, final TaskListener listener) throws IOException,
            InterruptedException {

        StandardUsernamePasswordCredentials scanCredentials = getScanCredentials();
        if (scanCredentials == null) {
            listener.getLogger().format("Connecting to %s with no credentials, anonymous access%n", gogsServerUrl == null ? "https://gogs.org" : gogsServerUrl);
        } else {
            listener.getLogger().format("Connecting to %s using %s%n", gogsServerUrl == null ? "https://gogs.org" : gogsServerUrl, CredentialsNameProvider.name(scanCredentials));
        }

        // Search branches
        retrieveBranches(observer, listener);
    }

    private void retrieveBranches(@NonNull final SCMHeadObserver observer, @NonNull TaskListener listener)
            throws IOException, InterruptedException {
        String fullName = repoOwner + "/" + repository;
        listener.getLogger().println("Looking up " + fullName + " for branches");

        final GogsApi gogs = getGogsConnector().create(repoOwner, repository, getScanCredentials());
        List<? extends GogsBranch> branches = gogs.getBranches();
        for (GogsBranch branch : branches) {
            listener.getLogger().println("Checking branch " + branch.getName() + " from " + fullName);
            final String branchName = branch.getName();
            if (isExcluded(branchName)) {
                continue;
            }
            SCMSourceCriteria criteria = getCriteria();
            if (criteria != null) {
                SCMSourceCriteria.Probe probe = getProbe(branchName, "branch", listener);
                if (criteria.isHead(probe, listener)) {
                    listener.getLogger().format("    Met criteria%n");
                } else {
                    listener.getLogger().format("    Does not meet criteria%n");
                    continue;
                }
            }
            SCMHead head = new SCMHead(branchName);
            SCMRevision hash = new AbstractGitSCMSource.SCMRevisionImpl(head, branch.getCommit().getHash());
            observer.observe(head, hash);

        }
    }


    /**
     * Returns a {@link jenkins.scm.api.SCMSourceCriteria.Probe} for use in {@link #retrieveBranches}.
     *
     * @param branch branch name
     * @param thing readable name of what this is, e.g. {@code branch}
     * @param listener A TaskListener to log useful information
     *
     * @return A {@link jenkins.scm.api.SCMSourceCriteria.Probe}
     */
    protected SCMSourceCriteria.Probe getProbe(final String branch, final String thing, final TaskListener listener) {
        return new SCMSourceCriteria.Probe() {
            private static final long serialVersionUID = 5012552654534124387L;
            @Override public String name() {
                return branch;
            }
            @Override public long lastModified() {
                return 0; // TODO
            }
            @Override public boolean exists(@Nonnull String path) throws IOException {
                final GogsApi gogs = getGogsConnector().create(repoOwner, repository, getScanCredentials());
                boolean exists = gogs.checkPathExists(branch, path);

                if(exists) {
                    return true;
                } else {
                    listener.getLogger().format("      ‘%s’ does not exist in this %s%n", path, thing);
                    return false;
                }
            }
        };
    }

    @Override
    protected SCMRevision retrieve(SCMHead head, TaskListener listener) throws IOException, InterruptedException {
        GogsApi gogs = getGogsConnector().create(repoOwner, repository, getScanCredentials());
        GogsBranch branch = gogs.getBranch(head.getName());
        listener.getLogger().println("Retrieving HEAD for " + branch.getName() + " branch");
        if (branch.getCommit() != null) {
            return new AbstractGitSCMSource.SCMRevisionImpl(head, branch.getCommit().getHash());
        }
        LOGGER.warning("No branch found in " + repoOwner + "/" + repository + " with name [" + head.getName() + "]");
        return null;
    }

    @Override
    public SCM build(SCMHead head, SCMRevision revision) {
        LOGGER.info("Build HEAD for " + head.getName() + " branch");
        if (revision == null) {
            // TODO will this work sanely for PRs? Branch.scm seems to be used only as a fallback for SCMBinder/SCMVar where they would perhaps better just report an error.
            return super.build(head, null);
        } else {
            return super.build(head, /* casting just as an assertion */(AbstractGitSCMSource.SCMRevisionImpl) revision);
        }
    }

    @Override
    protected List<RefSpec> getRefSpecs() {
        return new ArrayList<>(Arrays.asList(new RefSpec("+refs/heads/*:refs/remotes/origin/*"),
                // For PRs we check out the head, then perhaps merge with the base branch.
                new RefSpec("+refs/pull/*/head:refs/remotes/origin/pr/*")));
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @CheckForNull
    /* package */ StandardUsernamePasswordCredentials getScanCredentials() {
        return getGogsConnector().lookupCredentials(getOwner(), credentialsId, StandardUsernamePasswordCredentials.class);
    }

    private StandardCredentials getCheckoutCredentials() {
        return getGogsConnector().lookupCredentials(getOwner(), getCheckoutEffectiveCredentials(), StandardCredentials.class);
    }

    public String getRemoteName() {
      return "origin";
    }

    /**
     * Returns the pattern corresponding to the branches containing wildcards. 
     * 
     * @param branches space separated list of expressions. 
     *        For example "*" which would match all branches and branch* would match branch1, branch2, etc.
     * @return pattern corresponding to the branches containing wildcards (ready to be used by {@link Pattern})
     */
    private String getPattern(String branches) {
        StringBuilder quotedBranches = new StringBuilder();
        for (String wildcard : branches.split(" ")) {
            StringBuilder quotedBranch = new StringBuilder();
            for (String branch : wildcard.split("\\*")) {
                if (wildcard.startsWith("*") || quotedBranches.length() > 0) {
                    quotedBranch.append(".*");
                }
                quotedBranch.append(Pattern.quote(branch));
            }
            if (wildcard.endsWith("*")) {
                quotedBranch.append(".*");
            }
            if (quotedBranches.length() > 0) {
                quotedBranches.append("|");
            }
            quotedBranches.append(quotedBranch);
        }
        return quotedBranches.toString();
    }

    /**
     * Returns a {@link RepositoryUriResolver} according to credentials configuration.
     *
     * @return a {@link RepositoryUriResolver}
     */
    public RepositoryUriResolver getUriResolver() {
        String credentialsId = getCredentialsId();
        if (credentialsId == null) {
            return new HttpsRepositoryUriResolver();
        } else {
            if (getCredentials(StandardCredentials.class, credentialsId) instanceof SSHUserPrivateKey) {
                return new SshRepositoryUriResolver();
            } else {
                // Defaults to HTTP/HTTPS
                return new HttpsRepositoryUriResolver();
            }
        }
    }


    /**
     * Returns a credentials by type and identifier.
     *
     * @param type Type that we are looking for
     * @param credentialsId Identifier of credentials
     * @return The credentials or null if it does not exists
     */
    private <T extends StandardCredentials> T getCredentials(@Nonnull Class<T> type, @Nonnull String credentialsId) {
        return CredentialsMatchers.firstOrNull(CredentialsProvider.lookupCredentials(
                type, getOwner(), ACL.SYSTEM,
                Collections.<DomainRequirement> emptyList()), CredentialsMatchers.allOf(
                CredentialsMatchers.withId(credentialsId),
                CredentialsMatchers.instanceOf(type)));
    }

    private String getCheckoutEffectiveCredentials() {
        if (DescriptorImpl.ANONYMOUS.equals(checkoutCredentialsId)) {
            return null;
        } else if (DescriptorImpl.SAME.equals(checkoutCredentialsId)) {
            return credentialsId;
        } else {
            return checkoutCredentialsId;
        }
    }

    @Extension
    public static class DescriptorImpl extends SCMSourceDescriptor {

        public static final String ANONYMOUS = "ANONYMOUS";
        public static final String SAME = "SAME";

        @Override
        public String getDisplayName() {
            return "Gogs";
        }

        public FormValidation doCheckCredentialsId(@QueryParameter String value) {
            if (!value.isEmpty()) {
                return FormValidation.ok();
            } else {
                return FormValidation.warning("Credentials are required for notifications");
            }
        }

        public static FormValidation doCheckGogsServerUrl(@QueryParameter String gogsServerUrl) {
            String url = Util.fixEmpty(gogsServerUrl);
            if (url == null) {
                return FormValidation.ok();
            }
            try {
                new URL(gogsServerUrl);
            } catch (MalformedURLException e) {
                return FormValidation.error("Invalid URL: " +  e.getMessage());
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath SCMSourceOwner context, @QueryParameter String gogsServerUrl) {
            StandardListBoxModel result = new StandardListBoxModel();
            result.withEmptySelection();
            new GogsApiConnector(gogsServerUrl).fillCredentials(result, context);
            return result;
        }

        public ListBoxModel doFillCheckoutCredentialsIdItems(@AncestorInPath SCMSourceOwner context, @QueryParameter String gogsServerUrl) {
            StandardListBoxModel result = new StandardListBoxModel();
            result.add("- same as scan credentials -", SAME);
            result.add("- anonymous -", ANONYMOUS);
            new GogsApiConnector(gogsServerUrl).fillCheckoutCredentials(result, context);
            return result;
        }

    }
}
