package com.cloudbees.jenkins.plugins.gogs;

import com.cloudbees.jenkins.plugins.gogs.api.GogsRepository;
import hudson.model.InvisibleAction;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Invisible property on {@link WorkflowMultiBranchProject}
 * that retains information about Gogs repository.
 *
 * @author Kurt Madel
 */
public class GogsRepoAction extends InvisibleAction {
    private final URL url;
    private final String description;

    GogsRepoAction(GogsRepository repo) throws MalformedURLException {
        this.url = new URL(repo.getHtmlUrl());
        this.description = repo.getDescription();
    }

    public URL getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }
}
