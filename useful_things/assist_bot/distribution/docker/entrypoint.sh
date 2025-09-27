#!/bin/sh

java \
-Dbb.login=$BB_USERNAME \
-Dbb.password=$BB_PASSWORD \
-Dnexus.login=$NEXUS_USERNAME \
-Dnexus.password=$NEXUS_PASSWORD \
-Dbot.token=$BOT_TOKEN \
-Dapp.token=$APP_TOKEN \
-jar \
/opt/spring-boot-app/regressman_bot.jar