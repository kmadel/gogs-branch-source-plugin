package com.cloudbees.jenkins.plugins.gogs.server.client.repository;

/**
 * User role in repository:
 * <ul>
 *   <li>owner: returns all repositories owned by the current user.</li>
 *   <li>admin: returns repositories to which the user has explicit administrator access.</li>
 *   <li>contributor: returns repositories to which the user has explicit write access.<li>
 *   <li>member: returns repositories to which the user has explicit read access.</li>
 * </ul>
 *
 * See <a href="https://confluence.atlassian.com/bitbucket/repositories-endpoint-423626330.html#repositoriesEndpoint-GETalistofrepositoriesforanaccount">API docs</a> for more information.
 */
public enum UserRoleInRepository {

    OWNER("owner"),
    ADMIN("admin"),
    CONTRIBUTOR("contributor"),
    MEMBER("member");

    private String id;

    private UserRoleInRepository(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

}