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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import jenkins.scm.api.SCMHead;

/**
 * {@link SCMHead} extended with additional information:
 * <ul>
 *   <li>{@link #repoOwner}: the repository owner</li>
 *   <li>{@link #repoName}: the repository name</li>
 *   <li>{@link #pullRequestId}: the pull request ID (if it is representing a PR, null otherwise)</li>
 * </ul>
 * This information is required in this plugin since {@link GogsSCMSource} is processing pull requests
 * and they are managed as separate repositories in Gogs without any reference to them in the destination
 * repository.
 */
public class SCMHeadWithOwnerAndRepo extends SCMHead {

    private static final long serialVersionUID = 1L;

    private final String repoOwner;

    private final String repoName;

    private final Integer pullRequestId;

    private static final String PR_BRANCH_PREFIX = "PR-";

    public SCMHeadWithOwnerAndRepo(String repoOwner, String repoName, String branchName, Integer pullRequestId) {
        super(branchName);
        this.repoOwner = repoOwner;
        this.repoName = repoName;
        this.pullRequestId = pullRequestId;
    }

    public SCMHeadWithOwnerAndRepo(String repoOwner, String repoName, String branchName) {
        this(repoOwner, repoName, branchName, null);
    }

    public String getRepoOwner() {
        return repoOwner;
    }

    public String getRepoName() {
        return repoName;
    }

    /**
     * @return the original branch name without the "PR-owner-" part.
     */
    public String getBranchName() {
        return super.getName();
    }

    /**
     * Returns the prettified branch name by adding "PR-[ID]" if the branch is coming from a PR.
     * Use {@link #getBranchName()} to get the real branch name.
     */
    @Override
    public String getName() {
        return pullRequestId != null ? PR_BRANCH_PREFIX + pullRequestId : getBranchName();
    }

    @CheckForNull
    public Integer getPullRequestId() {
        return pullRequestId;
    }

}
