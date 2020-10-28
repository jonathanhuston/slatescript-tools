FROM openjdk:11
COPY ./target/uberjar/slatescript-tools-0.1.0-SNAPSHOT-standalone.jar /usr/app/
WORKDIR /usr/app
ENTRYPOINT [ "java", "--illegal-access=deny", "-jar", "slatescript-tools-0.1.0-SNAPSHOT-standalone.jar" ]
