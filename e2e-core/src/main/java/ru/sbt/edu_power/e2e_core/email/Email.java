package ru.sbt.edu_power.e2e_core.email;

import lombok.Getter;
import lombok.Setter;

// Класс реализует хранилище элементов электронного письма
@Getter
@Setter
public class Email {
    private String topic;
    private String message;
    private String messageBody;
    private String activationLink;
    private String smsCode;
    private String from;
}
