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

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import com.cloudbees.jenkins.plugins.gogs.api.GogsCommit;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GogsServerCommit implements GogsCommit {

    private String message;

    @JsonProperty("id")
    private String hash;

    public GogsServerCommit() {
    }

    public GogsServerCommit(String message, String hash) {
        this.message = message;
        this.hash = hash;
    }

    public GogsServerCommit(String hash) {
        this.hash = hash;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getHash() {
        return hash;
    }


    public void setMessage(String message) {
        this.message = message;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

}
