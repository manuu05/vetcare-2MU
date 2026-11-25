package it.univaq.vetcare.service;

import it.univaq.vetcare.dao.AppointmentDAO;
import it.univaq.vetcare.model.ClinicAppointment;
import it.univaq.vetcare.model.Veterinarian;
import it.univaq.vetcare.model.VisitType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * US-11 (Sub-Task US11-2): Servizio di business per la gestione dell'agenda unica
 * giornaliera e settimanale affiancata per i 3 veterinari.
 * Allineato al refactoring RF-01: un unico metodo centralizzato rileva i conflitti,
 * bookAppointment() solleva solo l'eccezione, mentre suggestNextAvailableSlot() può
 * essere invocato separatamente nel blocco catch del chiamante.
 */
public class ClinicAgendaService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentDAO appointmentDAO;

    public ClinicAgendaService(AppointmentDAO appointmentDAO) {
        this.appointmentDAO = appointmentDAO;
    }

    public ClinicAppointment bookAppointment(int animalId,
                                             int veterinarianId,
                                             VisitType visitType,
                                             LocalDateTime startDateTime,
                                             String clinicalNotes) {
        if (animalId <= 0) {
            throw new ValidationException("Selezionare un paziente valido per la prenotazione.");
        }
        Veterinarian vet = Veterinarian.fromId(veterinarianId);
        if (visitType == null || startDateTime == null) {
            throw new ValidationException("Specificare tipo di visita e data/ora di inizio.");
        }

        LocalDateTime endDateTime = startDateTime.plusMinutes(visitType.getDurationMinutes());
        List<ClinicAppointment> dailyAppointments = appointmentDAO.findByDay(startDateTime.toLocalDate());

        Optional<ClinicAppointment> conflict = findConflictingAppointment(
                dailyAppointments, veterinarianId, startDateTime, endDateTime);
        if (conflict.isPresent()) {
            ClinicAppointment existing = conflict.get();
            throw new ValidationException("CONFLITTO AGENDA VETERINARIO: " + vet.getFullName()
                    + " è già impegnato dalle " + existing.getStartDateTime().format(TIME_FMT)
                    + " alle " + existing.getEndDateTime().format(TIME_FMT)
                    + " (" + existing.getVisitType().getLabel() + " - Paziente: " + existing.getAnimalName() + ").");
        }

        ClinicAppointment toSave = new ClinicAppointment(
                null, animalId, null, veterinarianId, visitType,
                startDateTime, endDateTime, clinicalNotes, "PROGRAMMATO"
        );
        return appointmentDAO.insert(toSave);
    }

    private Optional<ClinicAppointment> findConflictingAppointment(List<ClinicAppointment> dailyAppointments,
                                                                   int veterinarianId,
                                                                   LocalDateTime start,
                                                                   LocalDateTime end) {
        for (ClinicAppointment existing : dailyAppointments) {
            if (existing.getVeterinarianId() == veterinarianId && existing.overlapsWith(start, end)) {
                return Optional.of(existing);
            }
        }
        return Optional.empty();
    }

    /**
     * Metodo invocabile esplicitamente nel blocco catch della GUI o dei servizi
     * per ottenere il primo orario libero successivo sullo stesso veterinario.
     */
    public LocalDateTime suggestNextAvailableSlot(int veterinarianId,
                                                  LocalDateTime requestedStart,
                                                  VisitType visitType) {
        List<ClinicAppointment> daily = appointmentDAO.findByDay(requestedStart.toLocalDate());
        int durationMinutes = visitType.getDurationMinutes();
        LocalDateTime candidate = requestedStart.plusMinutes(15);

        while (true) {
            LocalDateTime candidateEnd = candidate.plusMinutes(durationMinutes);
            Optional<ClinicAppointment> conflict = findConflictingAppointment(
                    daily, veterinarianId, candidate, candidateEnd);
            if (!conflict.isPresent()) {
                return candidate;
            }
            candidate = conflict.get().getEndDateTime();
        }
    }

    public List<ClinicAppointment> getDailyAgenda(LocalDate date) {
        return appointmentDAO.findByDay(date);
    }

    public List<ClinicAppointment> getDailyAgendaForVet(LocalDate date, int veterinarianId) {
        return appointmentDAO.findByDay(date).stream()
                .filter(a -> a.getVeterinarianId() == veterinarianId)
                .collect(Collectors.toList());
    }

    public List<ClinicAppointment> getWeeklyAgenda(LocalDate weekStartDate) {
        return appointmentDAO.findByDateRange(weekStartDate, weekStartDate.plusDays(6));
    }
}
