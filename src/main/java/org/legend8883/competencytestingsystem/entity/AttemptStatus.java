package org.legend8883.competencytestingsystem.entity;

public enum AttemptStatus {
    IN_PROGRESS,        // В процессе
    COMPLETED,          // Завершен (только закрытые вопросы)
    SUBMITTED,          // Отправлен на проверку (есть открытые вопросы)
    EVALUATING,         // На проверке у HR
    EVALUATED,          // Проверен HR
    AUTO_SUBMITTED      // Автоотправка по таймеру
}
