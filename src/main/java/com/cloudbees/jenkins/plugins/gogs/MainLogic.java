package com.cloudbees.jenkins.plugins.gogs;

import com.cloudbees.jenkins.plugins.gogs.api.GogsApi;
import com.cloudbees.jenkins.plugins.gogs.api.GogsOrganization;
import com.cloudbees.jenkins.plugins.gogs.api.GogsRepository;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.BulkChange;
import hudson.Extension;
import hudson.model.*;
import hudson.model.listeners.ItemListener;
import hudson.util.DescribableList;
import hudson.views.JobColumn;
import hudson.views.ListViewColumn;
import hudson.views.StatusColumn;
import hudson.views.WeatherColumn;
import jenkins.branch.OrganizationFolder;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static java.util.Arrays.asList;

/**
 * Main logic of UI customization.
 *
 * @author Kurt Madel
 */
@Extension
public class MainLogic {


    /**
     * Applies UI customizations to {@link OrganizationFolder} for Gogs
     */
    public void applyOrg(OrganizationFolder of, GogsSCMNavigator scm) throws IOException {
        if (UPDATING.get().add(of)) {
            BulkChange bc = new BulkChange(of);
            try {
                StandardUsernamePasswordCredentials credentials = GogsApiConnector.lookupScanCredentials(of, scm.getGogsServerUrl(), scm.getCredentialsId());
                LOGGER.info("creating connector with gogsServerUrl: " + scm.getGogsServerUrl() + " and repoOwner: " + scm.getRepoOwner());
                GogsApi gogs = GogsApiConnector.connect(scm.getGogsServerUrl(), scm.getRepoOwner(), credentials);

                GogsOrganization org = gogs.getOrganization();
                LOGGER.info("successfully retrieved org name: " + org.getName());

                of.setIcon(new GogsOrgIcon());
                of.replaceAction(new GogsOrgAction(org));
                of.setDescription(org.getDescription());

                //TODO clean up org url creation
                // build Gogs Org URL from avatarUrl
                URL endpoint = new URL(org.getAvatarUrl());
                String orgUrl = endpoint.getProtocol() + "://" + endpoint.getHost() + ":" + endpoint.getPort() + "/" + scm.getRepoOwner();
                of.replaceAction(new GogsLink("logo",orgUrl));
                if (of.getDisplayNameOrNull()==null)
                    of.setDisplayName(org.getDisplayName());
                if (of.getView("Repositories")==null && of.getView("All") instanceof AllView) {
                    // need to set the default view
                    ListView lv = new ListView("Repositories");
                    lv.getColumns().replaceBy(asList(
                            new StatusColumn(),
                            new WeatherColumn(),
                            new CustomNameJobColumn(Messages.class,Messages._ListViewColumn_Repository()),
                            new RepositoryDescriptionColumn()
                    ));
                    lv.setIncludeRegex(".*");   // show all
                    of.addView(lv);
                    of.deleteView(of.getView("All"));
                    of.setPrimaryView(lv);
                }

                bc.commit();
            } finally {
                bc.abort();
                UPDATING.get().remove(of);
            }
        }
    }

    /**
     * Applies UI customizations to a level below {@link OrganizationFolder}, which maps to a repository.
     */
    public void applyRepo(WorkflowMultiBranchProject item, GogsSCMNavigator scm) throws IOException {
        if (UPDATING.get().add(item)) {
            BulkChange bc = new BulkChange(item);
            try {

                StandardUsernamePasswordCredentials credentials = GogsApiConnector.lookupScanCredentials(item, scm.getGogsServerUrl(), scm.getCredentialsId());
                GogsApi gogs = GogsApiConnector.connect(scm.getGogsServerUrl(), scm.getRepoOwner(), item.getName(), credentials);

                GogsRepository repo = gogs.getRepository();

                item.setIcon(new GogsRepoIcon());
                item.replaceAction(new GogsRepoAction(repo));
                item.replaceAction(new GogsLink("repo",repo.getHtmlUrl()));
                if (item.getView("Branches")==null && item.getView("All") instanceof AllView) {
                    // create initial views
                    ListView bv = new ListView("Branches");
                    DescribableList<ListViewColumn, Descriptor<ListViewColumn>> cols = bv.getColumns();
                    JobColumn name = cols.get(JobColumn.class);
                    if (name!=null)
                        cols.replace(name,new CustomNameJobColumn(Messages.class, Messages._ListViewColumn_Branch()));

                    bv.getJobFilters().add(new BranchJobFilter());

                    item.addView(bv);
                    item.deleteView(item.getView("All"));
                    item.setPrimaryView(bv);
                }

                bc.commit();
            } finally {
                bc.abort();
                UPDATING.get().remove(item);
            }
        }
    }

    public static MainLogic get() {
        return Jenkins.getActiveInstance().getInjector().getInstance(MainLogic.class);
    }

    /**
     * Keeps track of what we are updating to avoid recursion, because {@link AbstractItem#save()}
     * triggers {@link ItemListener}.
     */
    private final ThreadLocal<Set<Item>> UPDATING = new ThreadLocal<Set<Item>>() {
        @Override
        protected Set<Item> initialValue() {
            return new HashSet<>();
        }
    };

    private static final Logger LOGGER = Logger.getLogger(MainLogic.class.getName());
}