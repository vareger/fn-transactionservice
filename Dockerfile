FROM java:8-jre
ADD ./build/libs/TransactionService-1.0-SNAPSHOT.jar app.jar
VOLUME /tmp
VOLUME /target
RUN bash -c 'touch /app.jar'
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]