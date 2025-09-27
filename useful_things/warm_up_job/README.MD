Модуль для выполнения прогрева стендов с использованием Smoke джобы

Модуль принимает на вход URL прошедшей Smoke джобы и минимальный процент прохождения

Если процент прохождения тестов не удовлетворительный, то модуль выкидывает исключение.

Расчёт процента прохождения выполняется по успешным шагам, а не тестам

Для запуска приложения вручную и отладки, использовать команду maven

mvn exec:java -Dexec.cleanupDaemonThreads=false -DjobUrl=/url/to/smoke/job/build -DexpectedPassedPercent=98 -Dnexus.login=${USERNAME} -Dnexus.password=${PASSWORD}