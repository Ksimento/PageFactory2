#language:ru

Функционал: Survey

  Сценарий: Survey
#    Добавляет новый вопрос в конструкторе Survey со всеми настройками
    И добавляет survey вопрос с типом "Группа для выбора одного" и параметрами
      | Заголовок        | generate-stash#phrase3#rb_title                                                                                                            |
      | Ответы           | generate-stash#phrase3#rb_value_1; generate-stash#phrase3#rb_value_2; generate-stash#phrase3#rb_value_3; generate-stash#phrase3#rb_value_4 |
      | Правильный ответ | 4                                                                                                                                          |

    И добавляет survey вопрос с типом "Поле ввода" и параметрами
      | Заголовок        | generate-stash#phrase3#text_title          |
      | Правильный ответ | generate-stash#phrase3#text_correct_answer |

    И добавляет survey вопрос с типом "Группа для выбора нескольких" и параметрами
      | Заголовок        | generate-stash#phrase3#chb_title                                                                                                               |
      | Ответы           | generate-stash#phrase3#chb_value_1; generate-stash#phrase3#chb_value_2; generate-stash#phrase3#chb_value_3; generate-stash#phrase3#chb_value_4 |
      | Правильный ответ | 1#true & 2#true & 4#true                                                                                                                       |

    И добавляет survey вопрос с типом "Выбор изображения" и параметрами
      | Заголовок        | generate-stash#phrase3#single_image_title                                                                      |
      | Ответы           | generate#word10; generate-stash#word10#single_image_value_1; generate#word10; generate#word10; generate#word10 |
      | Правильный ответ | 2                                                                                                              |

    И добавляет survey вопрос с типом "Выбор изображения" и параметрами
      | Заголовок        | generate-stash#phrase3#multiple_image_title                                                                                                                                |
      | Multi Select     | Да                                                                                                                                                                         |
      | Ответы           | generate#word10; generate-stash#word10#multiple_image_value_2; generate-stash#word10#multiple_image_value_3; generate#word10; generate-stash#word10#multiple_image_value_5 |
      | Правильный ответ | 4#true & 3#false & stash#multiple_image_value_5#true & 1#true                                                                                                              |

    И добавляет survey вопрос с типом "Список" и параметрами
      | Заголовок        | generate-stash#phrase3#select_title                                                                                                                        |
      | Ответы           | generate-stash#phrase3#select_value_1; generate-stash#phrase3#select_value_2; generate-stash#phrase3#select_value_3; generate-stash#phrase3#select_value_4 |
      | Правильный ответ | 4                                                                                                                                                          |

    И добавляет survey вопрос с типом "Таблица (выбор одного)" и параметрами
      | Заголовок        | generate-stash#phrase3#matrix_single_title                                                                                         |
      | Столбцы          | generate-stash#phrase3#matrix_single_col_1; generate-stash#phrase3#matrix_single_col_2; generate-stash#phrase3#matrix_single_col_3 |
      | Строки           | generate-stash#phrase3#matrix_single_row_1; generate-stash#phrase3#matrix_single_row_2; generate-stash#phrase3#matrix_single_row_3 |
      | Правильный ответ | 1: 2; 2: 3; 3: 1                                                                                                                   |

    И добавляет survey вопрос с типом "Таблица (множественный выбор)" и параметрами
      | Заголовок        | generate-stash#phrase3#matrix_multiple_title                                                                                                                     |
      | Ответы           | generate-stash#word6#matrix_multiple_ans_1;  generate-stash#word6#matrix_multiple_ans_2;  generate-stash#word6#matrix_multiple_ans_3;                            |
      | Строки           | generate-stash#word6#matrix_multiple_row_1;  generate-stash#word6#matrix_multiple_row_2                                                                          |
      | Столбцы          | Список, generate-stash#word6#matrix_multiple_col_1; checkbox, generate-stash#word6#matrix_multiple_col_2; radiogroup, generate-stash#word6#matrix_multiple_col_3 |
      | Правильный ответ | stash#matrix_multiple_row_1:stash#matrix_multiple_col_1:2:2: stash#matrix_multiple_ans_1#true & 3#true:3 :2 ; 2:1:3:2:2#true&1#true:3:1                          |

    И добавляет survey вопрос с типом "Множественный выбор текста" и параметрами
      | Заголовок        | generate-stash#phrase3#multitext_title                                                                                                                                  |
      | Объекты          | generate-stash#phrase3#multitext_row_1; generate-stash#phrase3#multitext_row_2; generate-stash#phrase3#multitext_row_3                                                  |
      | Правильный ответ | stash#multitext_row_1: generate-stash#phrase3#multitext_ans_1; 2: generate-stash#phrase3#multitext_ans_2; stash#multitext_row_3: generate-stash#phrase3#multitext_ans_3 |

#    Заполнение (конфигурирование) активного в данный момент виджета
#    Выбрать нужный виджет можно шагом И пользователь выбирает вопрос "Вопрос" в конструкторе survey
    И заполняет параметры активного вопроса
      | Заголовок        | generate-stash#phrase3#single_image_title                                                                      |
      | Ответы           | generate#word10; generate-stash#word10#single_image_value_1; generate#word10; generate#word10; generate#word10 |
      | Правильный ответ | 2                                                                                                              |

#    Заполнение учеником ответов в Survey задании
#    В левом столбце номер вопроса, в правом ответ, который нужно заполнить
#    (в приведённом примере используется номер варианта ответа)
    И заполняет ответы на вопросы survey
      | 1 | 2                           |
      | 2 | 2#true                      |
      | 3 | 2                           |
      | 4 | 2                           |
      | 5 | 2                           |
      | 6 | 1:2;2:2                     |
      | 7 | 1:1:2:2:2:3:2;2:1:2:2:2:3:2 |
      | 8 | 1:2;2:2                     |

#     Проверяет отображение красных и зелёных рамок вокруг вопросов
#     В левом столбце номер вопроса, в правом ожидаемый результат проверки ответа
    И проверяет правильность ответов survey
      | 1 | Да  |
      | 2 | Да  |
      | 3 | Нет |
      | 4 | Да  |
      | 5 | Да  |
      | 6 | Нет |
      | 7 | Да  |
      | 8 | Да  |

#    Шаг проверяет соответствие номера варианта ответа с его содержимым в выбранном вопросе
#    В левом столбце номер варианта ответа, в правом - ожидаемый контент варианта
    И проверяет корректность отображения элементов survey в вопросе "Вопрос"
      | 1 | TEXT : stash#chb3_value_1     |
      | 2 | IFRAME : https://example.com/ |
      | 3 | TEXT : stash#chb3_value_3     |

#    Шаг находит нужный вопрос в конструкторе и кликает по нему, делая его активным
#    для дальнейшей работы с ним
    И выбирает вопрос "Название или номер вопроса" в конструкторе survey

#    Проверяет параметры вопроса в конструкторе на соответствие ожидаемым
    И проверяет survey вопрос "Название или номер вопроса" на соответствие параметрам
      | Заголовок        | generate-stash#phrase3#single_image_title                                                                      |
      | Ответы           | generate#word10; generate-stash#word10#single_image_value_1; generate#word10; generate#word10; generate#word10 |
      | Правильный ответ | 2                                                                                                              |