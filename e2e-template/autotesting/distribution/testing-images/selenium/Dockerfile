#https://bitbucket.pcbltools.ru/bitbucket/projects/DEVOPS/repos/base-image/browse репозиторий базовых образов
FROM harbor-dev.pcbltools.ru/base_images/standalone-chrome:88.0

ARG USER_ID
ARG GROUP_ID
WORKDIR /app

RUN if [ ${USER_ID:-0} -ne 0 ] && [ ${GROUP_ID:-0} -ne 0 ]; then \
    userdel -f seluser && \
    if getent group seluser ; then groupdel seluser; fi && \
    groupadd -g ${GROUP_ID} seluser && \
    useradd -l -u ${USER_ID} -g seluser seluser && \
    install -d -m 0755 -o seluser -g seluser /home/seluser && \
    chown -R \
        ${USER_ID}:${GROUP_ID} \
        /home/seluser \
        /app \
    ;fi

RUN locale-gen ru_RU.UTF-8
ENV LANG ru_RU.UTF-8
ENV LANGUAGE ru_RU:ru
ENV LC_ALL ru_RU.UTF-8
        
USER seluser
