ARG GIT_COMMIT=unknown
ARG GIT_BRANCH=unknown
ARG APP_VERSION=unknown

FROM eclipse-temurin:11-jdk as build-stage

RUN apt-get update && apt-get install ant -y
COPY . /usr/src/app
WORKDIR /usr/src/app
RUN ["ant","-f","build.xml"]
RUN ["ant","-Ddir.workspace=/usr/src/app","-Ddir.jarfile=/usr/src/app","-f","/usr/src/app/exportjarclient.xml"]

FROM eclipse-temurin:11-jdk as production-stage
WORKDIR /usr/src/app
COPY --from=build-stage /usr/src/app/cslmainclient.jar ./
COPY cslconf/ cslconf/
COPY csldata/ csldata/
COPY datafile/ datafile/ 
COPY idsdata/ idsdata/ 
COPY runconfig/ runconfig/
COPY runconfig/CSLConfigIDS_template.json runconfig/CSLConfigIDS.json
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
