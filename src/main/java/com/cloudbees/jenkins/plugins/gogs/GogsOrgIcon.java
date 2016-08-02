package com.cloudbees.jenkins.plugins.gogs;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.FolderIcon;
import com.cloudbees.hudson.plugins.folder.FolderIconDescriptor;
import hudson.Extension;
import hudson.model.Hudson;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;

/**
 * Shows Avatar icon from Gogs organization/user.
 *
 * @author Kurt Madel
 */
public class GogsOrgIcon extends FolderIcon {
    private AbstractFolder<?> folder;

    @DataBoundConstructor
    public GogsOrgIcon() {
    }

    @Override
    protected void setOwner(AbstractFolder<?> folder) {
        this.folder = folder;
    }

    @Override
    public String getImageOf(String s) {
        //TODO figure out how to always return correctly sized avatar if available
        String url = getAvatarUrl();
        if(url != null && (s.equalsIgnoreCase("48x48") || s.equalsIgnoreCase("64x64"))) {
            return url;
        } else {
            // fall back to the generic gogs org icon
            return Stapler.getCurrentRequest().getContextPath()+ Hudson.RESOURCE_PATH+"/plugin/gogs-branch-source/images/logo/"+s+".png";
        }
    }

    @Override
    public String getDescription() {
        return folder!=null ? folder.getName() : "Gogs";
    }

    private String getAvatarUrl() {
        if (folder==null)   return null;
        GogsOrgAction p = folder.getAction(GogsOrgAction.class);
        if (p==null)    return null;
        return p.getAvatar();
    }

    @Extension
    public static class DescriptorImpl extends FolderIconDescriptor {
        @Override
        public String getDisplayName() {
            return "Gogs Organization Avatar";
        }
    }
}