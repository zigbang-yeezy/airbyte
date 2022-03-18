ARG JDK_VERSION=17.0.1
ARG JDK_IMAGE_REPOSITORY=openjdk
ARG JDK_IMAGE_TAG=${JDK_VERSION}-slim
FROM ${JDK_IMAGE_REPOSITORY}:${JDK_IMAGE_TAG} AS scheduler

ENV APPLICATION airbyte-scheduler

WORKDIR /app

ADD bin/${APPLICATION}-0.35.57-alpha.tar /app

# wait for upstream dependencies to become available before starting server
ENTRYPOINT ["/bin/bash", "-c", "${APPLICATION}-0.35.57-alpha/bin/${APPLICATION}"]
