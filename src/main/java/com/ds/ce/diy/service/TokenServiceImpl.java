package com.ds.ce.diy.service;

import com.ds.ce.diy.domain.State;
import com.ds.ce.diy.domain.User;
import com.ds.ce.diy.domain.security.VerificationToken;
import com.ds.ce.diy.domain.security.VerificationTokenType;
import com.ds.ce.diy.repositories.UserRepository;
import com.ds.ce.diy.repositories.VerificationTokenRepository;
import com.ds.ce.diy.settings.AppSettings;
import com.ds.ce.diy.web.EntryPoint;
import com.ds.ce.diy.web.RequestUtils;
import com.ds.ce.diy.web.exceptions.EntityNotFoundException;
import com.ds.ce.diy.web.exceptions.InvalidTokenException;
import com.ds.ce.diy.web.exceptions.UserAlreadyRegisteredException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static com.ds.ce.diy.domain.security.VerificationTokenType.EMAIL_REGISTRATION;

@Service
class TokenServiceImpl implements TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenServiceImpl.class);

    private final AppSettings settings;
    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final MailService mailService;
    private final Configuration freeMarkerConfig;

    @Inject
    public TokenServiceImpl(AppSettings settings, UserRepository userRepository,
                            VerificationTokenRepository tokenRepository, MailService mailService, Configuration freeMarkerConfiguration) {
        this.settings = settings;
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.mailService = mailService;
        this.freeMarkerConfig = freeMarkerConfiguration;
    }

    @Override
    //TODO change template and mail subject according to the token type
    public VerificationToken sendByMail(User user, VerificationToken token) {
        Assert.notNull(token);
        Assert.notNull(user);

        Map<String, String> dataModel = new HashMap<>();
        dataModel.put("username", user.getFirstname());
        dataModel.put("url", RequestUtils.getRequestPath(EntryPoint.TOKENS + "/" + token.getToken()));

        Writer stringTemplate = new StringWriter();
        try {
            Template template = freeMarkerConfig.getTemplate("mail_registration.ftl");
            template.process(dataModel, stringTemplate);
        } catch (IOException | TemplateException e) {
            logger.error("unable to load the template file: {}", e.getMessage());
        }

        mailService.sendMail(user, settings.getEmail().getRegistration().getSubject(), stringTemplate.toString());

        return token;
    }

    @Override
    //TODO create a service which remove tokens when expired
    //TODO expiration setting only for registration
    public VerificationToken createUserToken(@NotNull User user, VerificationTokenType type) {
        Assert.notNull(user);
        Assert.notNull(type);

        if (!userRepository.exists(user.getId())) {
            throw new EntityNotFoundException("unknown user [" + user + "]");
        }

        AppSettings.Email settingsEmail = settings.getEmail();
        int expiration;
        switch (type) {
            case EMAIL_REGISTRATION:
            default:
                expiration = settingsEmail.getRegistration().getTokenExpiration();
                break;
        }

        VerificationToken token = new VerificationToken(user, type, expiration);
        user.addVerificationToken(token);
        userRepository.save(user);

        return token;
    }

    @Override
    public VerificationToken verifyToken(String base64EncodedToken) {
        VerificationToken token =
                tokenRepository.findByToken(new String(Base64.getDecoder().decode(base64EncodedToken)))
                               .orElseThrow(() -> new EntityNotFoundException("token doesn't exist"));

        if (!token.isValid()) {
            throw new InvalidTokenException();
        }

        VerificationTokenType type = token.getType();

        if (EMAIL_REGISTRATION == type && State.VALID == token.getUser().getState()) {
            throw new UserAlreadyRegisteredException();
        }

        return token;
    }

    @Override
    public void invalidate(VerificationToken verifiedToken) {
        verifiedToken.setVerified();
        tokenRepository.save(verifiedToken);
    }
}
