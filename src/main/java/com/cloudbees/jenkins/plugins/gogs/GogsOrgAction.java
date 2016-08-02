package com.cloudbees.jenkins.plugins.gogs;

import com.cloudbees.jenkins.plugins.gogs.api.GogsOrganization;
import hudson.model.InvisibleAction;
import jenkins.branch.OrganizationFolder;

import java.io.IOException;
import java.net.URL;

/**
 * Invisible {@link OrganizationFolder} property that
 * retains information about Gogs organization.
 *
 * @author Kurt Madel
 */
public class GogsOrgAction extends InvisibleAction {
    private final URL url;
    private final String name;
    private final String avatar;

    GogsOrgAction(GogsOrganization org) throws IOException {
        this.url = org.getHtmlUrl();
        this.name = org.getDisplayName();
        this.avatar = org.getAvatarUrl();
    }

    public URL getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public String getAvatar() {
        return avatar;
    }
}
