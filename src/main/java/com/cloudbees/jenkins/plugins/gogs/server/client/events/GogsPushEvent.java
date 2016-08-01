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
package com.cloudbees.jenkins.plugins.gogs.server.client.events;

import com.cloudbees.jenkins.plugins.gogs.server.client.repository.PayloadRepo;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import com.cloudbees.jenkins.plugins.gogs.api.GogsRepository;
import com.cloudbees.jenkins.plugins.gogs.server.client.repository.GogsServerRepository;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GogsPushEvent {

    private String ref;

    private PayloadRepo repository;

    public PayloadRepo getRepository() {
        return repository;
    }

    public String getRef() {
        return ref;
    }

    public void setRepository(PayloadRepo repository) {
        this.repository = repository;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

}