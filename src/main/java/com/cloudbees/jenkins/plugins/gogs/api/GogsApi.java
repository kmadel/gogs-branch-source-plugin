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
package com.cloudbees.jenkins.plugins.gogs.api;

import java.util.List;

import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * Provides access to a specific repository.
 * One API object needs to be created for each repository you want to work with.
 */
public interface GogsApi {

    /**
     * @return the repository owner name.
     */
    String getOwner();

    /**
     * @return the repository name.
     */
    String getRepositoryName();

    /**
     * @return the repository specified by {@link #getOwner()}/{@link #getRepositoryName()} 
     *      (or null if repositoryName is not set)
     */
    @CheckForNull
    GogsRepository getRepository();

    /**
     * @return the list of branches in the repository.
     */
    List<? extends GogsBranch> getBranches();

    /**
     * @return the get branch in repository.
     */
    GogsBranch getBranch(String name);

    /**
     * Register a webhook on the repository.
     *
     * @param hook the webhook object
     */
    void registerCommitWebHook(GogsWebHook hook);

    /**
     * Create issue on repository.
     *
     * @param issue the issue object
     */
    void createIssue(GogsIssue issue);

    /**
     * Remove the webhook (ID field required) from the repository.
     *
     * @param hook the webhook object
     */
    void removeCommitWebHook(GogsWebHook hook);

    /**
     * @return the list of webhooks registered in the repository.
     */
    List<? extends GogsWebHook> getWebHooks();

    /**
     * @return the organization of the current owner, or null if {@link #getOwner()} is not an organization ID.
     */
    @CheckForNull
    GogsOrganization getOrganization();

    /**
     * @return the Gogs user for the current owner.
     */
    @CheckForNull
    GogsRepositoryOwner getUser();

    /**
     * Returns all the repositories for the current owner (even if it's a regular user or an organization).
     *
     * @return all repositories for the current {@link #getOwner()}
     */
    List<? extends GogsRepository> getRepositories();

    /**
     * @return true if the repository ({@link #getOwner()}/{@link #getRepositoryName()}) is private, false otherwise
     *          (if it's public or does not exists).
     */
    boolean isPrivate();


    /**
     * @return true if the path exists for repository branch.
     */
    boolean checkPathExists(String branch, String path);

}