#https://bitbucket.pcbltools.ru/bitbucket/projects/DEVOPS/repos/base-image/browse репозиторий базовых образов
FROM harbor-dev.pcbltools.ru/base_images/java-spring-boot:8u191
# слой изменяющийся при каждой сборке
USER root
RUN apk add --no-cache fontconfig ttf-dejavu

USER spring-boot-app-user
COPY *-jar-with-dependencies.jar /opt/spring-boot-app/regressman_bot.jar
COPY entrypoint.sh ./entrypoint.sh

ENTRYPOINT ["./entrypoint.sh"]
