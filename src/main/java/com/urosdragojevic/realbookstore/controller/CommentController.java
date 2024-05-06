package com.urosdragojevic.realbookstore.controller;

import com.urosdragojevic.realbookstore.audit.AuditLogger;
import com.urosdragojevic.realbookstore.domain.Comment;
import com.urosdragojevic.realbookstore.domain.User;
import com.urosdragojevic.realbookstore.repository.CommentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class CommentController {
    private static final Logger LOG = LoggerFactory.getLogger(CommentController.class);

    private CommentRepository commentRepository;

    public CommentController(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @PostMapping(value = "/comments")
    @PreAuthorize("hasAuthority('ADD_COMMENT')")
    public String createComment(@ModelAttribute Comment comment, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        comment.setUserId(user.getId());
        commentRepository.create(comment);
        AuditLogger.getAuditLogger(CommentController.class).audit("Created comment for book with id: " + comment.getBookId());
        return "redirect:/books/" + comment.getBookId();
    }
}
