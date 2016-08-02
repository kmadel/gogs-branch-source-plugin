package com.cloudbees.jenkins.plugins.gogs;

import com.cloudbees.hudson.plugins.folder.FolderIcon;
import com.cloudbees.hudson.plugins.folder.FolderIconDescriptor;
import hudson.Extension;
import hudson.model.Hudson;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;

/**
 * {@link FolderIcon} that shows the gogs repository icon.
 *
 * @author Kohsuke Kawaguchi
 */
public class GogsRepoIcon extends FolderIcon {
    @DataBoundConstructor
    public GogsRepoIcon() {
    }

    @Override
    public String getImageOf(String s) {
        return Stapler.getCurrentRequest().getContextPath()+ Hudson.RESOURCE_PATH+"/plugin/gogs-branch-source/images/repo/"+s+".png";
    }

    @Override
    public String getDescription() {
        return "Repository";
    }

    @Extension
    public static class DescriptorImpl extends FolderIconDescriptor {
        @Override
        public String getDisplayName() {
            return "Gogs Repository Icon";
        }
    }
}