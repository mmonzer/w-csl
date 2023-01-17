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
# CMD ["java","-jar","cslmainclient.jar"]
COPY entrypoint.sh .
RUN chmod +x entrypoint.sh
ENTRYPOINT [ "./entrypoint.sh", "cslmainclient.jar" ]
