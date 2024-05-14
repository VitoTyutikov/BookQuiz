package org.vito.server.auth.service;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vito.server.auth.entity.BlackListToken;
import org.vito.server.auth.repo.BlackListTokenRepository;

import java.util.Optional;

@Service
public class BlackListTokenService {

    private final BlackListTokenRepository blackListTokenRepository;

    public BlackListTokenService(BlackListTokenRepository blackListTokenRepository) {
        this.blackListTokenRepository = blackListTokenRepository;
    }

    @Transactional
    public BlackListToken save(BlackListToken blackListToken) {
        return blackListTokenRepository.save(blackListToken);
    }

    public Optional<BlackListToken> findByToken(String token) {
        return Optional.ofNullable(blackListTokenRepository.findByToken(token));
    }

    public boolean isBlackListed(String token) {
        return blackListTokenRepository.findByToken(token) != null;
    }

    @Transactional
    public void delete(BlackListToken blackListToken) {
        blackListTokenRepository.delete(blackListToken);
    }
}