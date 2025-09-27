Модуль для запуска заданий
--------
 - Поиск устаревших веток
 - Построение отчёта о прохождении регресса с выгрузкой в Confluence
 - Удаление не запускавшихся тест-кейсов из тест-сета
 - Создание нового тест-сета

Для запуска модуля локально или для отладки нужно выполнить команду maven

Поиск устаревших веток
--

> clean compile exec:java -Dexec.cleanupDaemonThreads=false -Drepository=${REPOSITORY} -DtestManagerProcess=OLD_BRANCH_NOTIFICATION -DbbLogin=${USERNAME} -DbbPass=${PASSWORD}

Где:

${REPOSITORY}:
 - EDU_BACK
 - EDU_FRONT
 - S21_APPLICATION
 - S21_APPLICATION_EXAM
 - MFE_DEPLOY_VERSION

${USERNAME}: имя пользователя для авторизации в Bitbucket

${PASSWORD}: пароль для авторизации в Bitbucket

Построение отчёта о прохождении регресса с выгрузкой в Confluence
--

> mvn clean compile -U exec:java -DtestRunKey=${TEST_RUN_KEY} -DtestManagerProcess=REGRESS_REPORT -Dexec.cleanupDaemonThreads=false

Где:

${TEST_RUN_KEY}: ключ тест-сета, например EDU-C1230

Удаление не запускавшихся тест-кейсов из тест-сета
--

> mvn clean compile -U exec:java -DtestRunKey=${params.TEST_RUN_KEY} -DtestManagerProcess=REMOVE_NOT_EXECUTED_TEST_CASES -Dexec.cleanupDaemonThreads=false

Где:

${TEST_RUN_KEY}: ключ тест-сета, например EDU-C1230

Создание тест-сета
--

> clean compile exec:java -DtestRunPreset=${PRESET} -DriskLevel=${RISK_LEVEL} -DtestManagerProcess=TEST_RUN_CREATION -Dversion=${VERSION} -DjiraProjectKey=${PROJECT} -DskipRiskUpdate={false/true} -Dexec.cleanupDaemonThreads=false

Где:

${PRESET}:
 - FULL_REGRESS
 - FULL_REGRESS_MFE
 - SMALL_REGRESS
 - SMALL_REGRESS_MFE
 - HIGH_PRIORITY_ONLY
 - HIGH_PRIORITY_ONLY_MFE
 - AUTO_ONLY
 - AUTO_ONLY_MFE

${RISK_LEVEL}:
 - LOWEST
 - LOW
 - MEDIUM
 - HIGH
 - CRITICAL

${VERSION}: версия приложения, например r/46.0.0-rc3

${PROJECT}: проектная область, например EDU или S21

skipRiskUpdate: параметр указывает, нужно ли выполнить обновление поля Риск в тест-кейсах перед созданием тест-сета
