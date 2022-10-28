FROM eclipse-temurin:11-jdk as build-stage

RUN apt-get update && apt-get install ant -y
COPY . /usr/src/app
WORKDIR /usr/src/app
RUN ["ant","-f","build.xml"]
RUN ["ant","-Ddir.workspace=/usr/src/app","-Ddir.jarfile=/usr/src/app","-f","/usr/src/app/exportclient.xml"]

FROM eclipse-temurin:11-jdk as production-stage
WORKDIR /usr/src/app
COPY --from=build-stage /usr/src/app/cslmainclient221025.jar ./
COPY cslconf/ cslconf/
COPY csldata/ csldata/
COPY datafile/ datafile/ 
COPY idsdata/ idsdata/ 
COPY runconfig/ runconfig/
CMD ["java","-jar","cslmainclient221025.jar"]