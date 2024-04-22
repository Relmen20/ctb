!#bin/bash

VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

java -jar ./target/telegram-bot-$VERSION.jar \
          --spring.config.location=file:./application.yml