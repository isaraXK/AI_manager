package com.aimanager.profile;

import java.time.LocalDate;

record ProfileFields(
        String nickname,
        String gender,
        LocalDate birthDate,
        String occupation,
        String routine,
        String interests,
        String mbti,
        String zodiac,
        String bloodType,
        String selfIntro) {

    ProfileFields normalized() {
        return new ProfileFields(
                normalize(nickname), normalize(gender), birthDate, normalize(occupation),
                normalize(routine), normalize(interests), normalize(mbti), normalize(zodiac),
                normalize(bloodType), normalize(selfIntro));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
