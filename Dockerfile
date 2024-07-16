ARG GIT_COMMIT=unknown
ARG GIT_BRANCH=unknown
ARG APP_VERSION=unknown

FROM gradle:jdk17 as build-stage

RUN #apt-get update && apt-get install ant -y
COPY . /usr/w-csl
WORKDIR /usr/w-csl
RUN ["gradle","clean","build","jar","-b","app/buildClient.gradle","-x", "test"]
#RUN ["ant","-f","build.xml"]
#RUN ["ant","-Ddir.workspace=/usr/src/app","-Ddir.jarfile=/usr/src/app","-f","/usr/src/app/exportjarclient.xml"]

FROM eclipse-temurin:17.0.11_9-jre as production-stage
WORKDIR /usr/src/app
COPY --from=build-stage /usr/w-csl/app/build/libs/app.jar ./cslmainclient.jar
COPY app/src/main/resources/cslconf/ cslconf/
#COPY app/src/main/resources/csldata/ csldata/
COPY app/src/main/resources/datafile/ datafile/
COPY app/src/main/resources/idsdata/ idsdata/
COPY app/src/main/resources/resources/ resources/
COPY app/src/main/resources/runconfig/ runconfig/
COPY app/src/main/resources/runconfig/CSLConfigIDS_template.json runconfig/CSLConfigIDS.json

COPY entrypoint.sh .

ARG GIT_COMMIT
ARG GIT_BRANCH
ARG APP_VERSION
# define the GIT environment variables
ENV GIT_COMMIT=$GIT_COMMIT
ENV GIT_BRANCH=$GIT_BRANCH
ENV APP_VERSION=$APP_VERSION
# define the GIT labels
LABEL git.commit.id=$GIT_COMMIT
LABEL git.branch=$GIT_BRANCH
LABEL app.version=$APP_VERSION

RUN chmod +x entrypoint.sh
ENTRYPOINT [ "./entrypoint.sh", "cslmainclient.jar" ]
