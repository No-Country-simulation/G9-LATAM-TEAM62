# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM docker.io/library/eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copy the Maven wrapper and project descriptor first so dependency
# resolution is cached in its own layer and only re-runs when pom.xml changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

# Copy sources and build the application (skip tests in the image build).
# scripts/ holds the Python statement parser; the build packages it into the jar
# (see maven-resources-plugin in pom.xml), so it has to be present here.
COPY src/ src/
COPY scripts/ scripts/
RUN ./mvnw -B clean package -DskipTests

# ---- Runtime stage ----
FROM docker.io/library/eclipse-temurin:25-jre AS runtime
WORKDIR /app

# The bank-statement parser is a Python CLI the API shells out to (see
# StatementIngestionService). Without an interpreter and the libraries below,
# POST /api/transactions/upload-statement fails on every request — which is
# exactly what a JRE-only image did.
RUN apt-get update \
    && apt-get install --no-install-recommends -y python3 python3-venv \
    && rm -rf /var/lib/apt/lists/*

# PEP 668 marks the distribution's interpreter as externally managed, so pip
# refuses to install into it. The parser's dependencies get their own virtualenv;
# putting it first on PATH is what makes the "python3" the API spawns resolve to
# the environment that actually has pandas and pdfplumber.
ENV VIRTUAL_ENV=/opt/parser-venv
ENV PATH="$VIRTUAL_ENV/bin:$PATH"
RUN python3 -m venv "$VIRTUAL_ENV"

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# Run as an unprivileged user.
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

# The parser script itself travels inside the jar and is extracted on startup,
# so there is no scripts/ directory to copy into this stage.
COPY --from=build /app/target/fintech-api-0.0.1-SNAPSHOT.jar app.jar

# Where the Oracle driver looks for the Autonomous Database wallet. The wallet is
# a credential: it is mounted at run time (see docker-compose.yml), never baked
# into the image. Without it the app runs on the default in-memory "dev" profile.
ENV TNS_ADMIN=/app/wallet

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
