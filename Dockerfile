FROM maven:3.9.9-eclipse-temurin-21

WORKDIR /app

RUN apt-get update && apt-get install -y \
    libgtk-3-0 \
    libx11-6 \
    libxtst6 \
    libxrender1 \
    libxi6 \
    libasound2t64 \
    && rm -rf /var/lib/apt/lists/*

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package

CMD ["mvn", "javafx:run"]