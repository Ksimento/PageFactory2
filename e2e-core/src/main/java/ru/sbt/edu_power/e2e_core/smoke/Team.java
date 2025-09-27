package ru.sbt.edu_power.e2e_core.smoke;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Аннотация необходима для тегирования пейджа команды
 * Пример:
 *    @Team(Teams.T_SC_Core_BE)
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Team(Teams.T_SC_FT_1)
public @interface Team {
    // Список команд
    Teams[] value();
}
