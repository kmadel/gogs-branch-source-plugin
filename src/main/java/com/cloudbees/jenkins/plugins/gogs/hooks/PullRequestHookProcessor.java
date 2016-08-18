package com.cloudbees.jenkins.plugins.gogs.hooks;

import com.cloudbees.jenkins.plugins.gogs.server.client.GogsWebhookPayload;
import com.cloudbees.jenkins.plugins.gogs.server.client.events.GogsPushEvent;

import java.util.logging.Logger;

public class PullRequestHookProcessor extends HookProcessor {

    private static final Logger LOGGER = Logger.getLogger(PushHookProcessor.class.getName());

    @Override
    public void process(String payload) {
        if (payload != null) {
            // TODO: generalize this for BB server
            GogsPushEvent push = GogsWebhookPayload.pushEventFromPayload(payload);
            if (push != null) {
                String owner = push.getRepository().getOwner().getUsername();
                String repository = push.getRepository().getName();

                LOGGER.info(String.format("Received hook from Gogs. Processing push event on %s/%s", owner, repository));
                scmSourceReIndex(owner, repository);
            }
        }
    }

}
