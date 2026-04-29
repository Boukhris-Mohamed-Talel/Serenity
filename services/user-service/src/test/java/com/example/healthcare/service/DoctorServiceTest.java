package com.example.healthcare.service;

import com.example.healthcare.dto.DoctorResponseDTO;
import com.example.healthcare.dto.DoctorUpdateRequest;
import com.example.healthcare.dto.UserResponseDTO;
import com.example.healthcare.entity.Doctor;
import com.example.healthcare.entity.Role;
import com.example.healthcare.entity.User;
import com.example.healthcare.mapper.DoctorMapper;
import com.example.healthcare.repository.DoctorRepository;
import com.example.healthcare.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    @Mock private DoctorRepository doctorRepository;
    @Mock private UserRepository userRepository;
    @Mock private RedisPublisher redisPublisher;
    @Mock private DoctorMapper doctorMapper;
    @Mock private UserService userService;

    @InjectMocks private DoctorService doctorService;

    @AfterEach
    void cleanupUploads() throws IOException {
        Path uploads = Paths.get("uploads");
        if (!Files.exists(uploads)) return;
        try (Stream<Path> walk = Files.walk(uploads)) {
            walk.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
        }
        Files.deleteIfExists(uploads);
    }

    @Test
    void createDoctorForExistingUser_whenUserNotDoctor_throws() {
        User u = new User();
        u.setId(1L);
        u.setRole(Role.PATIENT);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        MultipartFile img = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});

        assertThatThrownBy(() -> doctorService.createDoctorForExistingUser(1L, "cardio", img))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("does not have the DOCTOR role");
    }

    @Test
    void createDoctorForExistingUser_happyPath_publishesEvent() throws IOException {
        User u = new User();
        u.setId(2L);
        u.setEmail("doc@x.com");
        u.setPassword("pw");
        u.setRole(Role.DOCTOR);
        when(userRepository.findById(2L)).thenReturn(Optional.of(u));

        Doctor saved = new Doctor();
        saved.setId(2L);
        saved.setEmail("doc@x.com");
        when(doctorRepository.save(any(Doctor.class))).thenReturn(saved);

        DoctorResponseDTO dto = DoctorResponseDTO.builder()
                .id(2L)
                .email("doc@x.com")
                .build();
        when(doctorMapper.toDTO(saved)).thenReturn(dto);
        when(userService.uploadAvatar(eq("doc@x.com"), any(MultipartFile.class))).thenReturn(new UserResponseDTO());

        MultipartFile img = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2});

        DoctorResponseDTO result = doctorService.createDoctorForExistingUser(2L, "cardio", img);

        assertThat(result).isSameAs(dto);
        verify(userRepository).delete(u);
        verify(redisPublisher).publishDoctorEvent(dto);
        assertThat(Files.exists(Paths.get("uploads"))).isTrue();
    }

    @Test
    void createDoctorForExistingUser_whenUserMissing_throws() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());
        MultipartFile img = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});
        assertThatThrownBy(() -> doctorService.createDoctorForExistingUser(9L, "x", img))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void getAllDoctors_mapsAll() {
        Doctor d = new Doctor();
        d.setId(1L);
        when(doctorRepository.findAll()).thenReturn(List.of(d));
        when(doctorMapper.toDTO(d)).thenReturn(DoctorResponseDTO.builder().id(1L).build());
        assertThat(doctorService.getAllDoctors()).hasSize(1);
    }

    @Test
    void getDoctorById_emptyOptional() {
        when(doctorRepository.findById(1L)).thenReturn(Optional.empty());
        assertThat(doctorService.getDoctorById(1L)).isEmpty();
    }

    @Test
    void updateDoctor_updatesFields() {
        Doctor existing = new Doctor();
        existing.setId(3L);
        when(doctorRepository.findById(3L)).thenReturn(Optional.of(existing));
        Doctor patch = new Doctor();
        patch.setSpecialty("neuro");
        patch.setFirstName("F");
        when(doctorRepository.save(existing)).thenReturn(existing);
        when(doctorMapper.toDTO(existing)).thenReturn(DoctorResponseDTO.builder().id(3L).build());

        DoctorResponseDTO out = doctorService.updateDoctor(3L, patch);
        assertThat(out.getId()).isEqualTo(3L);
        assertThat(existing.getSpecialty()).isEqualTo("neuro");
        assertThat(existing.getFirstName()).isEqualTo("F");
    }

    @Test
    void deleteDoctor_delegates() {
        doctorService.deleteDoctor(5L);
        verify(doctorRepository).deleteById(5L);
    }

    @Test
    void verify_setsActive() {
        Doctor d = new Doctor();
        d.setId(2L);
        d.setIsActive(false);
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(d));
        doctorService.Verify(2L);
        assertThat(d.getIsActive()).isTrue();
        verify(doctorRepository).save(d);
    }

    @Test
    void getDoctorEmail_returnsEmail() {
        Doctor d = new Doctor();
        d.setEmail("e@doc.com");
        when(doctorRepository.findById(8L)).thenReturn(Optional.of(d));
        assertThat(doctorService.getDoctorEmail(8L)).isEqualTo("e@doc.com");
    }

    @Test
    void updateDoctorFull_withoutImage_updatesProfileFields() throws IOException {
        Doctor d = new Doctor();
        d.setId(4L);
        d.setEmail("doc@full.com");
        when(doctorRepository.findById(4L)).thenReturn(Optional.of(d));
        when(doctorRepository.save(d)).thenReturn(d);
        when(doctorMapper.toDTO(d)).thenReturn(DoctorResponseDTO.builder().id(4L).build());

        DoctorUpdateRequest req = new DoctorUpdateRequest();
        req.setFirstName("NewFirst");
        req.setBio("1234567890 bio here");
        req.setImage(null);

        DoctorResponseDTO out = doctorService.updateDoctorFull(4L, req);
        assertThat(out.getId()).isEqualTo(4L);
        assertThat(d.getFirstName()).isEqualTo("NewFirst");
        verify(userService, never()).uploadAvatar(anyString(), any(MultipartFile.class));
    }
}

