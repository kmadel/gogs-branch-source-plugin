package com.cloudbees.jenkins.plugins.gogs.notification;

import com.cloudbees.jenkins.plugins.gogs.GogsApiConnector;
import com.cloudbees.jenkins.plugins.gogs.GogsSCMSource;
import com.cloudbees.jenkins.plugins.gogs.Messages;
import com.cloudbees.jenkins.plugins.gogs.api.GogsApi;
import com.cloudbees.jenkins.plugins.gogs.api.GogsRepository;
import com.cloudbees.jenkins.plugins.gogs.server.client.issues.GogsServerIssue;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import hudson.model.listeners.SCMListener;
import hudson.scm.SCM;
import hudson.scm.SCMRevisionState;
import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Manages Gogs build status via issues.
 *
 * Job (associated to a PR) scheduled: PENDING
 * Build doing a checkout: PENDING
 * Build done: SUCCESS, FAILURE or ERROR
 *
 */
public class GogsBuildStatusNotification {

    private static final Logger LOGGER = Logger.getLogger(GogsBuildStatusNotification.class.getName());

    private static GogsServerIssue createCommitStatus(@Nonnull GogsRepository repo, @Nonnull String revision, @Nonnull GogsCommitState state, @Nonnull String url, @Nonnull String message, @Nonnull Job<?,?> job, int buildFailureLabelId) throws IOException {
        LOGGER.log(Level.FINE, "{0}/commit/{1} {2} from {3}", new Object[] {repo.getHtmlUrl(), revision, state, url});
        GogsServerIssue issue = new GogsServerIssue();

        String title = String.format("BUILD %s for commit: %s branch: %s", state.toString(), revision.substring(0,7), job.getName());
        String body = String.format("Commit: %s<br>%s<br>Build URL: %s", revision, message, url);

        issue.setTitle(title);
        issue.setBody(body);
        if(buildFailureLabelId > 0)
            issue.setLabels(Arrays.asList(buildFailureLabelId));

        return issue;
    }

    @SuppressWarnings("deprecation") // Run.getAbsoluteUrl appropriate here
    private static void createBuildCommitStatus(Run<?,?> build, TaskListener listener) {
        try {
            SCMSourceOwner scmSourceOwner = getSCMSourceOwner(build.getParent());
            //no need to continue if there is no SCMSourceOwner
            if(scmSourceOwner != null) {
                GogsSCMSource source = getSCMSource(scmSourceOwner);
                int buildFailureLabelId = source.getBuildFailureLabelId();

                GogsApi gogs = GogsApiConnector.connect(source.getGogsServerUrl(), source.getRepoOwner(), source.getRepository(), GogsApiConnector.lookupScanCredentials
                        (scmSourceOwner, null, source.getCredentialsId()));

                GogsRepository repo = gogs.getRepository();
                if (repo != null) {
                    List<Cause> causes = build.getCauses();
                    for (Cause cause : causes) {
                        LOGGER.info(cause.getClass().getName() + " cause short desc: " + cause.getShortDescription());
                    }

                    SCMRevisionAction action = build.getAction(SCMRevisionAction.class);
                    if (action != null) {
                        SCMRevision revision = action.getRevision();
                        String url;
                        try {
                            url = build.getAbsoluteUrl();
                        } catch (IllegalStateException ise) {
                            url = "http://unconfigured-jenkins-location/" + build.getUrl();
                        }
                        boolean ignoreError = false;
                        try {
                            Result result = build.getResult();
                            String revisionToNotify = resolveHeadCommit(revision);
                            Job<?, ?> job = build.getParent();
                            GogsServerIssue issue = null;
                            if (Result.UNSTABLE.equals(result)) {
                                issue = createCommitStatus(repo, revisionToNotify, GogsCommitState.FAILURE, url, Messages.GogsBuildStatusNotification_CommitStatus_Unstable(), job, buildFailureLabelId);
                            } else if (Result.FAILURE.equals(result)) {
                                issue = createCommitStatus(repo, revisionToNotify, GogsCommitState.FAILURE, url, Messages.GogsBuildStatusNotification_CommitStatus_Failure(), job, buildFailureLabelId);
                            } else if (!Result.SUCCESS.equals(result) && result != null) { // ABORTED etc.
                                issue = createCommitStatus(repo, revisionToNotify, GogsCommitState.ERROR, url, Messages.GogsBuildStatusNotification_CommitStatus_Other(), job, buildFailureLabelId);
                            }
                            if (issue != null) {
                                LOGGER.info("create issue with title: " + issue.getTitle());
                                gogs.createIssue(issue);
                            }
                            if (result != null) {
                                listener.getLogger().format("%n" + Messages.GogsBuildStatusNotification_CommitStatusSet() + "%n%n");
                            }
                        } catch (FileNotFoundException fnfe) {
                            if (!ignoreError) {
                                listener.getLogger().format("%nCould not update commit status, please check if your scan " +
                                        "credentials belong to a member of the organization or a collaborator of the " +
                                        "repository and repo:status scope is selected%n%n");
                                LOGGER.log(Level.FINE, null, fnfe);
                            }
                        }
                    }
                }
            }
        } catch (IOException ioe) {
            listener.getLogger().format("%nCould not update commit status. Message: %s%n%n", ioe.getMessage());
            LOGGER.log(Level.FINE, "Could not update commit status", ioe);
        }

    }

    /**
     * Returns the GitHub Repository associated to a Job.
     *
     * @param job A {@link Job}
     * @return A {@link GogsApi} or null, either if a scan credentials was not provided, or a GitHubSCMSource was not defined.
     * @throws IOException
     */
    private static @CheckForNull
    SCMSourceOwner getSCMSourceOwner(@Nonnull Job<?,?> job) throws IOException {
        ItemGroup<?> multiBranchProject = job.getParent();
        if (multiBranchProject instanceof SCMSourceOwner) {
            return (SCMSourceOwner) multiBranchProject;
        }
        return null;
    }

    /**
     * It is possible having more than one SCMSource in our MultiBranchProject.
     * TODO: Does it make sense having more than one of the same type?
     *
     * @param scmSourceOwner An {@link Item} that owns {@link SCMSource} instances.
     * @return A {@link GogsSCMSource} or null
     */
    @CheckForNull
    private static GogsSCMSource getSCMSource(final SCMSourceOwner scmSourceOwner) {
        for (SCMSource scmSource : scmSourceOwner.getSCMSources()) {
            if (scmSource instanceof GogsSCMSource) {
                return (GogsSCMSource) scmSource;
            }
        }
        return null;
    }


    /**
     * With this listener one notifies to GitHub when the SCM checkout process has started.
     * Possible option: GHCommitState.PENDING
     */
    @Extension public static class JobCheckOutListener extends SCMListener {

        @Override public void onCheckout(Run<?, ?> build, SCM scm, FilePath workspace, TaskListener listener, File changelogFile, SCMRevisionState pollingBaseline) throws Exception {
            createBuildCommitStatus(build, listener);
        }

    }

    /**
     * With this listener one notifies to GitHub the build result.
     * Possible options: GHCommitState.SUCCESS, GHCommitState.ERROR or GHCommitState.FAILURE
     */
    @Extension public static class JobCompletedListener extends RunListener<Run<?,?>> {

        @Override public void onCompleted(Run<?, ?> build, TaskListener listener) {
            createBuildCommitStatus(build, listener);
        }

    }

    private static String resolveHeadCommit(SCMRevision revision) throws IllegalArgumentException {
        if (revision instanceof SCMRevisionImpl) {
            return ((SCMRevisionImpl) revision).getHash();
        } else {
            throw new IllegalArgumentException("did not recognize " + revision);
        }
    }

    private GogsBuildStatusNotification() {}

    public enum GogsCommitState {
        PENDING, SUCCESS, ERROR, FAILURE
    }

}