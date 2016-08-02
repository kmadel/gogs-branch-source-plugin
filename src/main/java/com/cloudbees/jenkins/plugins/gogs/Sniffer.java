package com.cloudbees.jenkins.plugins.gogs;

import com.cloudbees.jenkins.plugins.gogs.GogsSCMNavigator;
import hudson.model.Item;
import java.util.List;
import jenkins.branch.OrganizationFolder;
import jenkins.scm.api.SCMNavigator;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import jenkins.branch.Branch;

/**
 * Pattern matching on Gogs organization folder member {@link Item}s.
 *
 * @author Kurt Madel
 */
class Sniffer {
    static class OrgMatch {
        final OrganizationFolder folder;
        final GogsSCMNavigator scm;

        public OrgMatch(OrganizationFolder folder, GogsSCMNavigator scm) {
            this.folder = folder;
            this.scm = scm;
        }
    }

    public static OrgMatch matchOrg(Object item) {
        if (item instanceof OrganizationFolder) {
            OrganizationFolder of = (OrganizationFolder)item;
            List<SCMNavigator> navigators = of.getNavigators();
            if (/* could be called from constructor */navigators != null && navigators.size() > 0) {
                SCMNavigator n = navigators.get(0);
                if (n instanceof GogsSCMNavigator) {
                    return new OrgMatch(of, (GogsSCMNavigator) n);
                }
            }
        }
        return null;
    }

    static class RepoMatch extends OrgMatch {
        final WorkflowMultiBranchProject repo;

        public RepoMatch(OrgMatch x, WorkflowMultiBranchProject repo) {
            super(x.folder,x.scm);
            this.repo = repo;
        }
    }

    public static RepoMatch matchRepo(Object item) {
        if (item instanceof WorkflowMultiBranchProject) {
            WorkflowMultiBranchProject repo = (WorkflowMultiBranchProject)item;
            OrgMatch org = matchOrg(repo.getParent());
            if (org!=null)
                return new RepoMatch(org, repo);
        }
        return null;
    }

    static class BranchMatch extends RepoMatch {
        final WorkflowJob branch;

        public BranchMatch(RepoMatch x, WorkflowJob branch) {
            super(x,x.repo);
            this.branch = branch;
        }

        public Branch getScmBranch() {
            return repo.getProjectFactory().getBranch(branch);
        }
    }

    public static BranchMatch matchBranch(Item item) {
        if (item instanceof WorkflowJob) {
            WorkflowJob branch = (WorkflowJob)item;
            RepoMatch x = matchRepo(item.getParent());
            if (x!=null)
                return new BranchMatch(x,branch);
        }
        return null;
    }

}
