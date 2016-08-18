Gogs Branch Source
=====================
**[Pipeline as Code](https://go.cloudbees.com/docs/cloudbees-documentation/cookbook/ch19.html#ch19_pipeline-as-code) for Gogs: Go Git Service!**

A Jenkins Plugin that provides SCMSource (i.e. [Pipeline Multibranch](https://wiki.jenkins-ci.org/display/JENKINS/Pipeline+Multibranch+Plugin)) and SCMNavigator for [Gogs - Go Git Service](https://github.com/gogits/gogs).

## Features

- Supports Pipeline Multibranch
- Auto creation of repository webhooks for `push` and `create` events
- Supports SCMNavigator (Gogs Organization Scanning) functionality (i.e. org scanning per [GitHub Organization Folder](https://wiki.jenkins-ci.org/display/JENKINS/GitHub+Organization+Folder+Plugin))
- Supports commit status update via repo issue creation on build FAILURE and UNSTABLE - Gogs currently doesn't support commit statuses see https://github.com/gogits/gogs/issues/813
- Partial support for Gogs Organization avatar - Gogs does not support dynamically sized avatar images

### Missing Features

- Does not support Gogs Pull Requests as there is currently no API - see https://github.com/gogits/gogs/issues/2246
- Does not support auto-creation of Organization webhooks as there is currently no API - see https://github.com/gogits/go-gogs-client/issues/38
- Update commit status - see https://github.com/gogits/gogs/issues/813

### Use with CJP Trial

There is a [prepopulated Gogs server Docker image](https://hub.docker.com/r/beedemo/gogs/) availalbe on Docker Hub. If you start this container and connect it to the same Docker network that is created when running the Dockerized CJP trial you will be able to experinece Jenkins Pipeline as Code in the lovely confines of your personal computer ;) Here is an example Docker command:

```
docker run -d --name=gogs --net="cjptrial_default" -p 10022:22 -p 10080:3000 beedemo/gogs:develop
````
Note: your network name may vary, especially on windows (`docker network ls`)

#### Tested Against

- Gogs: Go Git Service 0.9.60.0803
- Git Version: 2.6.6
