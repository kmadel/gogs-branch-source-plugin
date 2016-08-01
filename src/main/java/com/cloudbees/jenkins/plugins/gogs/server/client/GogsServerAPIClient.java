/*
 * The MIT License
 *
 * Copyright (c) 2016, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.cloudbees.jenkins.plugins.gogs.server.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.cloudbees.jenkins.plugins.gogs.api.GogsApi;
import com.cloudbees.jenkins.plugins.gogs.api.GogsRepository;
import com.cloudbees.jenkins.plugins.gogs.api.GogsRequestException;
import com.cloudbees.jenkins.plugins.gogs.api.GogsOrganization;
import com.cloudbees.jenkins.plugins.gogs.api.GogsWebHook;
import com.cloudbees.jenkins.plugins.gogs.server.client.repository.UserRoleInRepository;
import com.cloudbees.jenkins.plugins.gogs.server.client.branch.GogsServerBranch;
import com.cloudbees.jenkins.plugins.gogs.server.client.repository.GogsServerOrganization;
import com.cloudbees.jenkins.plugins.gogs.server.client.repository.GogsServerRepositories;
import com.cloudbees.jenkins.plugins.gogs.server.client.repository.GogsServerRepository;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

import hudson.ProxyConfiguration;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.codehaus.jackson.map.type.CollectionType;

/**
 * Gogs API client.
 * Developed and tested with Gogs 0.9
 */
public class GogsServerAPIClient implements GogsApi {

    private static final Logger LOGGER = Logger.getLogger(GogsServerAPIClient.class.getName());
    private static final String API_BASE_PATH = "/api/v1";
    private static final String API_REPOSITORIES_PATH = API_BASE_PATH + "/repos/search?q=_&uid=%d";
    private static final String API_REPOSITORY_PATH = API_BASE_PATH + "/repos/%s/%s";
    private static final String API_BRANCHES_PATH = API_BASE_PATH + "/repos/%s/%s/branches";
    private static final String API_BRANCH_PATH = API_BASE_PATH + "/repos/%s/%s/branches/%s";
    private static final String API_ORGANIZATION_PATH = API_BASE_PATH + "/orgs/%s";

    /**
     * Repository owner.
     * This must be null if organization is not null.
     */
    private String owner;

    /**
     * Thre repository that this object is managing.
     */
    private String repositoryName;

    /**
     * Indicates if the client is using user-centric API endpoints or project API otherwise.
     */
    private boolean userCentric = false;

    /**
     * Credentials to access API services.
     * Almost @NonNull (but null is accepted for anonymous access).
     */
    private UsernamePasswordCredentials credentials;

    private String baseURL;

    public GogsServerAPIClient(String baseURL, String username, String password, String owner, String repositoryName, boolean userCentric) {
        if (!StringUtils.isBlank(username) && !StringUtils.isBlank(password)) {
            this.credentials = new UsernamePasswordCredentials(username, password);
        }
        this.userCentric = userCentric;
        this.owner = owner;
        this.repositoryName = repositoryName;
        this.baseURL = baseURL;
    }

    public GogsServerAPIClient(String baseURL, String owner, String repositoryName, StandardUsernamePasswordCredentials creds, boolean userCentric) {
        if (creds != null) {
            this.credentials = new UsernamePasswordCredentials(creds.getUsername(), Secret.toString(creds.getPassword()));
        }
        this.userCentric = userCentric;
        this.owner = owner;
        this.repositoryName = repositoryName;
        this.baseURL = baseURL;
    }

    public GogsServerAPIClient(String baseURL, String owner, StandardUsernamePasswordCredentials creds, boolean userCentric) {
        this(baseURL, owner, null, creds, userCentric);
    }

    /**
     * Gogs manages two top level entities, owner and/or organization.
     * Only one of them makes sense for a specific client object.
     */
    @Override
    public String getOwner() {
        return owner;
    }

    /**
     * In Gogs the top level entity is the Organization, but the JSON API accepts users as a replacement
     * of Organizations in most of the URLs (it's called user centric API).
     *
     * This method returns the appropriate string to be placed in request URLs taking into account if this client
     * object was created as a user centric instance or not.
     * 
     * @return the ~user or organization
     */
    public String getUserCentricOwner() {
        return userCentric ? "~" + owner : owner;
    }

    /** {@inheritDoc} */
    @Override
    public String getRepositoryName() {
        return repositoryName;
    }

    /** {@inheritDoc} */
    @Override
    public GogsRepository getRepository() {
        if (repositoryName == null) {
            return null;
        }
        String response = getRequest(String.format(API_REPOSITORY_PATH, getUserCentricOwner(), repositoryName));
        try {
            return parse(response, GogsServerRepository.class);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "invalid repository response.", e);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public List<GogsServerBranch> getBranches() {
        String url = String.format(API_BRANCHES_PATH, getUserCentricOwner(), repositoryName, 0);

        try {
            String response = getRequest(url);
            List<GogsServerBranch> branches = parseCollection(response, GogsServerBranch.class);

            return branches;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "invalid branches response", e);
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public GogsServerBranch getBranch(String name) {
        if (repositoryName == null) {
            return null;
        }
        String response = getRequest(String.format(API_BRANCH_PATH, getUserCentricOwner(), repositoryName, name));
        try {
            return parse(response, GogsServerBranch.class);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "invalid branch response.", e);
        }
        return null;

    }

    @Override
    public void registerCommitWebHook(GogsWebHook hook) {
        try {
            postRequest(String.format(API_REPOSITORY_PATH, getUserCentricOwner(), repositoryName) + "/hooks", asJson(hook));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "cannot register webhook", e);
        }
    }

    @Override
    public void removeCommitWebHook(GogsWebHook hook) {
        // TODO
    }

    @Override
    public List<GogsWebHook> getWebHooks() {
        try {
            String response = getRequest(String.format(API_REPOSITORY_PATH, getUserCentricOwner(), repositoryName) + "/hooks");
            List<GogsWebHook> repositoryHooks = parseCollection(response, GogsWebHook.class);
            return repositoryHooks;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "invalid hooks response", e);
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * There is no such Team concept in Bitbucket Server but Project.
     */
    @Override
    public GogsOrganization getOrganization() {
        if (userCentric) {
            return null;
        } else {
            String response = getRequest(String.format(API_ORGANIZATION_PATH, getOwner()));
            try {
                return parse(response, GogsServerOrganization.class);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "invalid organization response.", e);
            }
            return null;
        }
    }

    /**
     * The role parameter is ignored for Gogs.
     */
    @Override
    public List<GogsServerRepository> getRepositories(UserRoleInRepository role) {
        String url = String.format(API_REPOSITORIES_PATH, getUserCentricOwner(), 0);

        try {
            List<GogsServerRepository> repositories = new ArrayList<GogsServerRepository>();

            String response = getRequest(url);
            GogsServerRepositories page = parse(response, GogsServerRepositories.class);
            repositories.addAll(page.getValues());

            return repositories;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "invalid repositories response", e);
        }
        return Collections.EMPTY_LIST;
    }

    /** {@inheritDoc} */
    @Override
    public List<GogsServerRepository> getRepositories() {
        return getRepositories(null);
    }

    @Override
    public boolean isPrivate() {
        GogsRepository repo = getRepository();
        return repo != null ? repo.isPrivate() : false;
    }


    private <T> T parse(String response, Class<T> clazz) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(response, clazz);
    }


    private <T> List<T> parseCollection(String response, Class<T> clazz) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        final CollectionType javaType =
                mapper.getTypeFactory().constructCollectionType(List.class, clazz);
        return mapper.readValue(response, javaType);
    }

    private String getRequest(String path) {
        HttpClient client = getHttpClient();
        client.getState().setCredentials(AuthScope.ANY, credentials);
        GetMethod httpget = new GetMethod(this.baseURL + path);
        client.getParams().setAuthenticationPreemptive(true);
        String response = null;
        InputStream responseBodyAsStream = null;
        try {
            client.executeMethod(httpget);
            responseBodyAsStream = httpget.getResponseBodyAsStream();
            response = IOUtils.toString(responseBodyAsStream, "UTF-8");
            if (httpget.getStatusCode() != HttpStatus.SC_OK) {
                throw new GogsRequestException(httpget.getStatusCode(), "HTTP request error. Status: " + httpget.getStatusCode() + ": " + httpget.getStatusText() + ".\n" + response);
            }
        } catch (HttpException e) {
            throw new GogsRequestException(0, "Communication error: " + e, e);
        } catch (IOException e) {
            throw new GogsRequestException(0, "Communication error: " + e, e);
        } finally {
            httpget.releaseConnection();
            if (responseBodyAsStream != null) {
                IOUtils.closeQuietly(responseBodyAsStream);
            }
        }
        if (response == null) {
            throw new GogsRequestException(0, "HTTP request error " + httpget.getStatusCode() + ":" + httpget.getStatusText());
        }
        return response;
    }

    private HttpClient getHttpClient() {
        HttpClient client = new HttpClient();

        client.getParams().setConnectionManagerTimeout(10 * 1000);
        client.getParams().setSoTimeout(60 * 1000);

        Jenkins jenkins = Jenkins.getInstance();
        ProxyConfiguration proxy = null;
        if (jenkins != null) {
            proxy = jenkins.proxy;
        }
        if (proxy != null) {
            LOGGER.info("Jenkins proxy: " + proxy.name + ":" + proxy.port);
            client.getHostConfiguration().setProxy(proxy.name, proxy.port);
            String username = proxy.getUserName();
            String password = proxy.getPassword();
            if (username != null && !"".equals(username.trim())) {
                LOGGER.info("Using proxy authentication (user=" + username + ")");
                client.getState().setProxyCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));
            }
        }
        return client;
    }

    private int getRequestStatus(String path) {
        HttpClient client = getHttpClient();
        client.getState().setCredentials(AuthScope.ANY, credentials);
        GetMethod httpget = new GetMethod(this.baseURL + path);
        client.getParams().setAuthenticationPreemptive(true);
        try {
            client.executeMethod(httpget);
            return httpget.getStatusCode();
        } catch (HttpException e) {
            LOGGER.log(Level.SEVERE, "Communication error", e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Communication error", e);
        } finally {
            httpget.releaseConnection();
        }
        return -1;
    }

    private <T> String serialize(T o) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(o);
    }

    private String postRequest(String path, NameValuePair[] params) throws UnsupportedEncodingException {
        PostMethod httppost = new PostMethod(this.baseURL + path);
        httppost.setRequestEntity(new StringRequestEntity(nameValueToJson(params), "application/json", "UTF-8"));
        return postRequest(httppost);
    }

    private String postRequest(String path, String content) throws UnsupportedEncodingException {
        PostMethod httppost = new PostMethod(this.baseURL + path);
        httppost.setRequestEntity(new StringRequestEntity(content, "application/json", "UTF-8"));
        return postRequest(httppost);
    }

    private String nameValueToJson(NameValuePair[] params) {
        JSONObject o = new JSONObject();
        for (NameValuePair pair : params) {
            o.put(pair.getName(), pair.getValue());
        }
        return o.toString();
    }

    private String postRequest(PostMethod httppost) throws UnsupportedEncodingException {
        HttpClient client = getHttpClient();
        client.getState().setCredentials(AuthScope.ANY, credentials);
        client.getParams().setAuthenticationPreemptive(true);
        String response = null;
        InputStream responseBodyAsStream = null;
        try {
            client.executeMethod(httppost);
            if (httppost.getStatusCode() == HttpStatus.SC_NO_CONTENT) {
                // 204, no content
                return "";
            }
            responseBodyAsStream = httppost.getResponseBodyAsStream();
            if (responseBodyAsStream != null) {
                response = IOUtils.toString(responseBodyAsStream, "UTF-8");
            }
            if (httppost.getStatusCode() != HttpStatus.SC_OK && httppost.getStatusCode() != HttpStatus.SC_CREATED) {
                throw new GogsRequestException(httppost.getStatusCode(), "HTTP request error. Status: " + httppost.getStatusCode() + ": " + httppost.getStatusText() + ".\n" + response);
            }
        } catch (HttpException e) {
            throw new GogsRequestException(0, "Communication error: " + e, e);
        } catch (IOException e) {
            throw new GogsRequestException(0, "Communication error: " + e, e);
        } finally {
            httppost.releaseConnection();
            if (responseBodyAsStream != null) {
                IOUtils.closeQuietly(responseBodyAsStream);
            }
        }
        if (response == null) {
            throw new GogsRequestException(0, "HTTP request error");
        }
        return response;

    }

    private String asJson(GogsWebHook hook) throws JsonGenerationException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(hook);
    }

}
