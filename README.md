## Overview

A Jenkins Plugin that provides SCMSource (i.e. [Pipeline Multibranch](https://wiki.jenkins-ci.org/display/JENKINS/Pipeline+Multibranch+Plugin)) and SCMNavigator for [Gogs - Go Git Service](https://github.com/gogits/gogs)

##Features

- Supports Pipeline Multibranch
- Auto creation of repository push web hook
- Support SCMNavigator (Gogs Organization Scanning) functionality (i.e. org scanning per [GitHub Organization Folder](https://wiki.jenkins-ci.org/display/JENKINS/GitHub+Organization+Folder+Plugin))
- Commit status via repo issue creation on build FAILURE and UNSTABLE - Gogs currently doesn't support commit statuses see https://github.com/gogits/gogs/issues/813
- Partial support for Gogs Organization avatar - Gogs does not support dynamically sized avatar images

##Missing Features

- Does not support Gogs Pull Requests - see https://github.com/gogits/gogs/issues/2246
- Does not support auto-creation of Organization web hooks - see https://github.com/gogits/go-gogs-client/issues/38
- Update commit status - see https://github.com/gogits/gogs/issues/813
