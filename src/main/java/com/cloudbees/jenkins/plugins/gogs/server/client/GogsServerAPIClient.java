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
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cloudbees.jenkins.plugins.gogs.api.*;
import com.cloudbees.jenkins.plugins.gogs.server.client.repository.*;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URIException;
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
import org.codehaus.jackson.map.type.CollectionType;

import com.cloudbees.jenkins.plugins.gogs.server.client.branch.GogsServerBranch;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

import hudson.ProxyConfiguration;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

/**
 * Gogs API client.
 * Developed and tested with Gogs 0.9
 */
public class GogsServerAPIClient implements GogsApi {

    private static final Logger LOGGER = Logger.getLogger(GogsServerAPIClient.class.getName());
    private static final String API_BASE_PATH = "/api/v1";
    private static final String API_REPOSITORIES_PATH = API_BASE_PATH + "/repos/search?q=_&uid=%d&limit=%d";
    private static final String API_REPOSITORY_PATH = API_BASE_PATH + "/repos/%s/%s";
    private static final String API_BRANCHES_PATH = API_BASE_PATH + "/repos/%s/%s/branches";
    private static final String API_BRANCH_PATH = API_BASE_PATH + "/repos/%s/%s/branches/%s";
    private static final String API_ORGANIZATION_PATH = API_BASE_PATH + "/orgs/%s";
    private static final String API_USER_PATH = API_BASE_PATH + "/users/%s";
    private static final String API_CONTENT_PATH = API_BASE_PATH + "/repos/%s/%s/raw/%s/%s";
    private static final String API_ISSUES_PATH = API_BASE_PATH + "/repos/%s/%s/issues";
    private static final int DEFAULT_REPOSITORY_READ_LIMIT = 1000;

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

    public GogsServerAPIClient(String baseURL, String username, String password, String owner, String repositoryName) {
        if (!StringUtils.isBlank(username) && !StringUtils.isBlank(password)) {
            this.credentials = new UsernamePasswordCredentials(username, password);
        }
        this.owner = owner;
        this.repositoryName = repositoryName;
        this.baseURL = baseURL;
    }

    public GogsServerAPIClient(String baseURL, String owner, String repositoryName, StandardUsernamePasswordCredentials creds) {
        if (creds != null) {
            this.credentials = new UsernamePasswordCredentials(creds.getUsername(), Secret.toString(creds.getPassword()));
        }
        this.owner = owner;
        this.repositoryName = repositoryName;
        this.baseURL = baseURL;
    }

    public GogsServerAPIClient(String baseURL, String owner, StandardUsernamePasswordCredentials creds) {
        this(baseURL, owner, null, creds);
    }

    /**
     * Gogs manages two top level entities, owner and/or organization.
     * Only one of them makes sense for a specific client object.
     */
    @Override
    public String getOwner() {
        return owner;
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
        String response = getRequest(String.format(API_REPOSITORY_PATH, getOwner(), repositoryName));
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
        String url = String.format(API_BRANCHES_PATH, getOwner(), repositoryName, 0);

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
        String response = getRequest(String.format(API_BRANCH_PATH, getOwner(), repositoryName, name));
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
            postRequest(String.format(API_REPOSITORY_PATH, getOwner(), repositoryName) + "/hooks", asJson(hook));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "cannot register webhook", e);
        }
    }

    @Override
    public void createIssue(GogsIssue issue) {
        try {
            postRequest(String.format(API_ISSUES_PATH, getOwner(), repositoryName), serialize(issue));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "cannot create issue", e);
        }
    }

    @Override
    public void removeCommitWebHook(GogsWebHook hook) {
        // TODO
    }

    @Override
    public List<GogsRepositoryHook> getWebHooks() {
        try {
            String url = String.format(API_REPOSITORY_PATH, getOwner(), repositoryName) + "/hooks";
            LOGGER.info("getWebHooks url: " + url);
            String response = getRequest(url);
            List<GogsRepositoryHook> repositoryHooks = parseCollection(response, GogsRepositoryHook.class);
            return repositoryHooks;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "invalid hooks response", e);
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Gogs Organization.
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

    /** {@inheritDoc} */
    @Override
    public List<GogsServerRepository> getRepositories() {
        GogsRepositoryOwner user = getUser();
        String url = String.format(API_REPOSITORIES_PATH, user.getId(), DEFAULT_REPOSITORY_READ_LIMIT);

        try {
            String response = getRequest(url);
            GogsServerRepositories wrappedRepos = parse(response, GogsServerRepositories.class);

            return wrappedRepos.getData();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "invalid repositories response", e);
        }
        return Collections.EMPTY_LIST;
    }

    /** {@inheritDoc} */
    @Override
    public GogsRepositoryOwner getUser() {
        if (userCentric) {
            return null;
        } else {
            String response = getRequest(String.format(API_USER_PATH, getOwner()));
            try {
                return parse(response, GogsServerRepositoryOwner.class);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "invalid user response.", e);
            }
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean checkPathExists(String branch, String path) {
        String url = String.format(API_CONTENT_PATH, getOwner(), repositoryName, branch, path);
        LOGGER.info("checkPathExists url: " + url);
        int status = getRequestStatus(url);
        return status == HttpStatus.SC_OK;
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
        GetMethod httpget = new GetMethod(this.baseURL + path);
        HttpClient client = getHttpClient(getMethodHost(httpget));
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
    
    private static String getMethodHost(HttpMethod method) {
        try {
            return method.getURI().getHost();
        } catch (URIException e) {
            throw new IllegalStateException("Could not obtain host part for method " + method, e);
        }
    }    

    private HttpClient getHttpClient(String host) {
        HttpClient client = new HttpClient();

        client.getParams().setConnectionManagerTimeout(10 * 1000);
        client.getParams().setSoTimeout(60 * 1000);

        client.getState().setCredentials(AuthScope.ANY, credentials);
        client.getParams().setAuthenticationPreemptive(true);
        
        Jenkins jenkins = Jenkins.getInstance();
        ProxyConfiguration proxyConfig = null;
        if (jenkins != null) {
        	proxyConfig = jenkins.proxy;
        }
        
        Proxy proxy = Proxy.NO_PROXY;
        if (proxyConfig != null) {
        	// Takes noProxyHost into account while creating proxy from proxy configuration
            proxy = proxyConfig.createProxy(host);
        }
        
        if (proxy != Proxy.NO_PROXY) { // Use proxy
            final InetSocketAddress proxyAddress = (InetSocketAddress)proxy.address();
            LOGGER.log(Level.FINE, "Jenkins proxy: {0}", proxy.address());
            client.getHostConfiguration().setProxy(proxyAddress.getHostString(), proxyAddress.getPort());
            String username = proxyConfig.getUserName();
            String password = proxyConfig.getPassword();
            if (username != null && !"".equals(username.trim())) {
                LOGGER.log(Level.FINE, "Using proxy authentication (user={0})", username);
                client.getState().setProxyCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(username, password));
            }
        }
        
        return client;
    }

    private int getRequestStatus(String path) {
        GetMethod httpget = new GetMethod(this.baseURL + path);
        HttpClient client = getHttpClient(getMethodHost(httpget));
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
        String valueAsString = mapper.writeValueAsString(o);
        LOGGER.info("serialized value: " + valueAsString);
        return valueAsString;
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
        HttpClient client = getHttpClient(getMethodHost(httppost));
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
