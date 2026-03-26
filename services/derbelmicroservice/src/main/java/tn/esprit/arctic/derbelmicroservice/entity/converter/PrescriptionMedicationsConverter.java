package tn.esprit.arctic.derbelmicroservice.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tn.esprit.arctic.derbelmicroservice.entity.value.PrescriptionMedication;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Converter
public class PrescriptionMedicationsConverter implements AttributeConverter<List<PrescriptionMedication>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String ITEM_SEPARATOR = "\u001E";
    private static final String FIELD_SEPARATOR = "\u001F";

    @Override
    public String convertToDatabaseColumn(List<PrescriptionMedication> attribute) {
        List<PrescriptionMedication> safe = attribute == null ? Collections.emptyList() : attribute;
        try {
            List<SerializedMedication> payload = safe.stream()
                    .map(SerializedMedication::fromDomain)
                    .toList();
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialize prescription medications", e);
        }
    }

    @Override
    public List<PrescriptionMedication> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }

        String trimmed = dbData.trim();
        if ("[]".equals(trimmed)) {
            return new ArrayList<>();
        }

        // Preferred format: readable JSON array.
        if (trimmed.startsWith("[")) {
            try {
                List<SerializedMedication> payload = OBJECT_MAPPER.readValue(
                        trimmed,
                        new TypeReference<List<SerializedMedication>>() {
                        });
                return payload.stream().map(SerializedMedication::toDomain).toList();
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Unable to parse prescription medications JSON", e);
            }
        }

        // Legacy encoded format compatibility.
        if (trimmed.contains(FIELD_SEPARATOR) || trimmed.contains(ITEM_SEPARATOR)) {
            String[] items = trimmed.split(ITEM_SEPARATOR, -1);
            List<PrescriptionMedication> parsed = new ArrayList<>();
            for (String item : items) {
                if (!item.isBlank()) {
                    parsed.add(deserializeLegacyItem(item));
                }
            }
            return parsed;
        }

        // Legacy plain text fallback.
        return new ArrayList<>(List.of(PrescriptionMedication.builder()
                .medicationName(trimmed)
                .dosage("")
                .frequency("")
                .quantity(1)
                .status("ACTIVE")
                .build()));
    }

    private PrescriptionMedication deserializeLegacyItem(String serialized) {
        String[] fields = serialized.split(FIELD_SEPARATOR, -1);
        return PrescriptionMedication.builder()
                .medicationName(unb64(getField(fields, 0)))
                .dosage(unb64(getField(fields, 1)))
                .frequency(unb64(getField(fields, 2)))
                .startDate(parseDate(unb64(getField(fields, 3))))
                .endDate(parseDate(unb64(getField(fields, 4))))
                .instructions(unb64(getField(fields, 5)))
                .quantity(parseInt(unb64(getField(fields, 6)), 1))
                .status(unb64(getField(fields, 7)))
                .build();
    }

    private static String getField(String[] fields, int index) {
        return index < fields.length ? fields[index] : "";
    }

    private static String unb64(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
    }

    private static java.time.LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return java.time.LocalDate.parse(value);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static final class SerializedMedication {
        public String medicationName;
        public String dosage;
        public String frequency;
        public String startDate;
        public String endDate;
        public String instructions;
        public Integer quantity;
        public String status;

        static SerializedMedication fromDomain(PrescriptionMedication med) {
            SerializedMedication out = new SerializedMedication();
            out.medicationName = med.getMedicationName();
            out.dosage = med.getDosage();
            out.frequency = med.getFrequency();
            out.startDate = med.getStartDate() != null ? med.getStartDate().toString() : null;
            out.endDate = med.getEndDate() != null ? med.getEndDate().toString() : null;
            out.instructions = med.getInstructions();
            out.quantity = med.getQuantity();
            out.status = med.getStatus();
            return out;
        }

        PrescriptionMedication toDomain() {
            return PrescriptionMedication.builder()
                    .medicationName(medicationName)
                    .dosage(dosage)
                    .frequency(frequency)
                    .startDate(parseDate(startDate))
                    .endDate(parseDate(endDate))
                    .instructions(instructions)
                    .quantity(quantity != null ? quantity : 1)
                    .status(status)
                    .build();
        }
    }
}
