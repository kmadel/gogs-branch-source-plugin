package com.cloudbees.jenkins.plugins.gogs;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.TopLevelItem;
import hudson.model.View;
import hudson.views.ViewJobFilter;
import jenkins.scm.api.SCMHead;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;

/**
 * Show branch jobs.
 *
 * @author Kurt Madel
 */
public class BranchJobFilter extends ViewJobFilter {
    @DataBoundConstructor
    public BranchJobFilter() {}

    @Override
    public List<TopLevelItem> filter(List<TopLevelItem> added, List<TopLevelItem> all, View filteringView) {
        for (TopLevelItem i : all) {
            if (added.contains(i))      continue;   // already in there

            Sniffer.BranchMatch b = Sniffer.matchBranch(i);
            if (b!=null) {
                SCMHead head = b.getScmBranch().getHead();
                if (shouldShow(head))
                    added.add(i);
            }
        }
        return added;
    }

    protected boolean shouldShow(SCMHead head) {
        return true;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ViewJobFilter> {
        @Override
        public String getDisplayName() {
            return "Gogs Branch Jobs Only";
        }
    }

}