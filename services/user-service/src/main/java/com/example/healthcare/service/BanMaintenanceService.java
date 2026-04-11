package com.example.healthcare.service;

import com.example.healthcare.entity.User;
import com.example.healthcare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BanMaintenanceService {
    private static final Duration AUTO_UNBAN_GRACE_WINDOW = Duration.ofHours(3);

    private final UserRepository userRepository;

    @Transactional
    public int unbanExpiredUsers() {
        Date now = new Date();
        Date cutoff = new Date(now.getTime() + AUTO_UNBAN_GRACE_WINDOW.toMillis());
        List<User> eligibleUsers =
                userRepository.findByIsPermanentlyBannedFalseAndBannedUntilLessThanEqual(cutoff);
        if (eligibleUsers.isEmpty()) {
            return 0;
        }

        eligibleUsers.forEach(user -> {
            user.setBannedUntil(null);
            user.setIsPermanentlyBanned(false);
        });
        userRepository.saveAll(eligibleUsers);
        log.info("Auto-unbanned {} users with <=3h remaining or expired bans", eligibleUsers.size());
        return eligibleUsers.size();
    }
}
