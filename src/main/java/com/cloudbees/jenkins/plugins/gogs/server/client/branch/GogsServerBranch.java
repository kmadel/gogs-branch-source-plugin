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
package com.cloudbees.jenkins.plugins.gogs.server.client.branch;

import com.cloudbees.jenkins.plugins.gogs.api.GogsCommit;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import com.cloudbees.jenkins.plugins.gogs.api.GogsBranch;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GogsServerBranch implements GogsBranch {

    private String name;

    private GogsServerCommit commit;

    //Need to deal with repos that don't have any branches yet
    private String message;

    public GogsServerBranch() {
    }

    public GogsServerBranch(String name, GogsServerCommit commit) {
        this.name = name;
        this.commit = commit;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public GogsCommit getCommit() {
        return commit;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCommit(GogsServerCommit commit) {
        this.commit = commit;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
