package com.cloudbees.jenkins.plugins.gogs;

import hudson.model.Action;

import java.net.URL;

/**
 * Link to Gogs
 *
 * @author Kurt Madel
 */
public class GogsLink implements Action {
    /**
     * Maps to the directory name under webapp/images
     */
    private final String image;

    /**
     * Target of the hyperlink to take the user to.
     */
    private final String url;

    /*package*/ GogsLink(String image, String url) {
        this.image = image;
        this.url = url;
    }

    /*package*/ GogsLink(String image, URL url) {
        this(image,url.toExternalForm());
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String getIconFileName() {
        return "/plugin/gogs-branch-source/images/"+ image +"/24x24.png";
    }

    @Override
    public String getDisplayName() {
        return "Gogs";
    }

    @Override
    public String getUrlName() {
        return url;
    }
}