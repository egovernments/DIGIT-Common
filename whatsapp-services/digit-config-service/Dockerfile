# Build stage - uses official multi-arch JDK image
FROM eclipse-temurin:17-jdk-alpine AS build

ARG WORK_DIR=.

WORKDIR /app

# Install Maven
RUN apk add --no-cache maven

# Copy the project files
COPY ${WORK_DIR}/pom.xml ./pom.xml

COPY ${WORK_DIR}/src ./src
RUN mvn -B -f /app/pom.xml package -DskipTests

# Runtime stage - uses official multi-arch JRE image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /opt/egov

COPY --from=build /app/target/*.jar /opt/egov/

RUN addgroup -g 1000 egov && \
    adduser -u 1000 -G egov -s /bin/sh -D egov && \
    chown -R egov:egov /opt/egov

USER egov

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/configs/health || exit 1

CMD ["sh", "-c", "java ${JAVA_OPTS:--Xmx64m -Xms64m} ${JAVA_ARGS} -jar /opt/egov/*.jar"]
