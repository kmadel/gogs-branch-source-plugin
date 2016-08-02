package com.cloudbees.jenkins.plugins.gogs;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;

import javax.inject.Inject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hook to add decorations to Gogs organization folders.
 *
 * @author Kurt Madel
 */
@Extension
public class ItemListenerImpl extends ItemListener {
    @Inject
    private MainLogic main;

    @Override
    public void onUpdated(Item item) {
        maybeApply(item);
    }

    @Override
    public void onCreated(Item item) {
        maybeApply(item);
    }

    private void maybeApply(Item item) {
        try {
            Sniffer.OrgMatch f = Sniffer.matchOrg(item);
            if (f!=null) {
                main.applyOrg(f.folder, f.scm);
            }

            Sniffer.RepoMatch r = Sniffer.matchRepo(item);
            if (r!=null) {
                main.applyRepo(r.repo, r.scm);
            }

        } catch (FileNotFoundException e) {
            LOGGER.log(Level.FINE, "Failed to apply Gogs Org Folder theme to " + item.getFullName() +
                    " because the Org does not exists or it's not accessible", e);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to apply Gogs Org Folder theme to " + item.getFullName(), e);
        }
    }

    private static final Logger LOGGER = Logger.getLogger(ItemListenerImpl.class.getName());
}
