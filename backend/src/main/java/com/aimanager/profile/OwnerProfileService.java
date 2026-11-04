package com.aimanager.profile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!smoke")
class OwnerProfileService {
    static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final NamedParameterJdbcTemplate jdbc;

    OwnerProfileService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    ProfileResponse find(long ownerId) {
        return jdbc.query("""
                SELECT nickname, gender, birth_date, occupation, routine, interests,
                       mbti, zodiac, blood_type, self_intro, version, updated_at
                FROM owner_profile
                WHERE owner_id = :ownerId
                """, new MapSqlParameterSource("ownerId", ownerId), (row, index) -> {
                    LocalDate birthDate = row.getObject("birth_date", LocalDate.class);
                    var fields = new ProfileFields(
                            row.getString("nickname"), row.getString("gender"), birthDate,
                            row.getString("occupation"), row.getString("routine"), row.getString("interests"),
                            row.getString("mbti"), row.getString("zodiac"), row.getString("blood_type"),
                            row.getString("self_intro"));
                    int age = Period.between(birthDate, LocalDate.now(BUSINESS_ZONE)).getYears();
                    Instant updatedAt = row.getObject("updated_at", LocalDateTime.class).toInstant(ZoneOffset.UTC);
                    return new ProfileResponse(true, row.getLong("version"), fields, age, updatedAt);
                }).stream().findFirst().orElse(new ProfileResponse(false, 0, null, null, null));
    }

    @Transactional
    public ProfileResponse save(long ownerId, long version, ProfileFields fields) {
        var parameters = new MapSqlParameterSource()
                .addValue("ownerId", ownerId)
                .addValue("version", version)
                .addValue("nickname", fields.nickname())
                .addValue("gender", fields.gender())
                .addValue("birthDate", fields.birthDate())
                .addValue("occupation", fields.occupation())
                .addValue("routine", fields.routine())
                .addValue("interests", fields.interests())
                .addValue("mbti", fields.mbti())
                .addValue("zodiac", fields.zodiac())
                .addValue("bloodType", fields.bloodType())
                .addValue("selfIntro", fields.selfIntro())
                .addValue("now", LocalDateTime.now(ZoneOffset.UTC));

        if (version == 0) {
            try {
                jdbc.update("""
                        INSERT INTO owner_profile
                            (owner_id, nickname, gender, birth_date, occupation, routine, interests,
                             mbti, zodiac, blood_type, self_intro, version, created_at, updated_at)
                        VALUES
                            (:ownerId, :nickname, :gender, :birthDate, :occupation, :routine, :interests,
                             :mbti, :zodiac, :bloodType, :selfIntro, 1, :now, :now)
                        """, parameters);
            } catch (DuplicateKeyException conflict) {
                throw new VersionConflict();
            }
        } else {
            int updated = jdbc.update("""
                    UPDATE owner_profile
                    SET nickname = :nickname, gender = :gender, birth_date = :birthDate,
                        occupation = :occupation, routine = :routine, interests = :interests,
                        mbti = :mbti, zodiac = :zodiac, blood_type = :bloodType, self_intro = :selfIntro,
                        version = version + 1, updated_at = :now
                    WHERE owner_id = :ownerId AND version = :version
                    """, parameters);
            if (updated == 0) {
                throw new VersionConflict();
            }
        }
        return find(ownerId);
    }

    record ProfileResponse(boolean profileReady, long version, ProfileFields profile, Integer age, Instant updatedAt) {}

    static final class VersionConflict extends RuntimeException {}
}
