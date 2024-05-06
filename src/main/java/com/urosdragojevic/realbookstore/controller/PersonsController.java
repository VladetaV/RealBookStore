package com.urosdragojevic.realbookstore.controller;

import ch.qos.logback.core.joran.spi.ActionException;
import com.urosdragojevic.realbookstore.audit.AuditLogger;
import com.urosdragojevic.realbookstore.domain.Person;
import com.urosdragojevic.realbookstore.domain.User;
import com.urosdragojevic.realbookstore.repository.PersonRepository;
import com.urosdragojevic.realbookstore.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.sql.SQLException;
import java.util.List;

@Controller
public class PersonsController {

    private static final Logger LOG = LoggerFactory.getLogger(PersonsController.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(PersonRepository.class);

    private final PersonRepository personRepository;
    private final UserRepository userRepository;


    public PersonsController(PersonRepository personRepository, UserRepository userRepository) {
        this.personRepository = personRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/persons/{id}")
    @PreAuthorize("hasAuthority('VIEW_PERSON')")
    public String person(@PathVariable int id, Model model, HttpSession session) {
        model.addAttribute("person", personRepository.get("" + id));
        model.addAttribute("CSRF_TOKEN", session.getAttribute("CSRF_TOKEN"));
        return "person";
    }

    @GetMapping("/myprofile")
    @PreAuthorize("hasAuthority('VIEW_MY_PROFILE')")
    public String self(Model model, Authentication authentication, HttpSession session) {
        User user = (User) authentication.getPrincipal();
        model.addAttribute("person", personRepository.get("" + user.getId()));
        model.addAttribute("CSRF_TOKEN", session.getAttribute("CSRF_TOKEN"));
        return "person";
    }

    @DeleteMapping("/persons/{id}")
    @PreAuthorize("hasAuthority('UPDATE_PERSON')")
    public ResponseEntity<Void> person(@PathVariable int id) throws ActionException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User usr = (User) auth.getPrincipal();
        int usrId = usr.getId();
        boolean hasauth = usr.getAuthorities().contains(new SimpleGrantedAuthority("UPDATE_PERSON"));
        if(usrId == id || hasauth) {
            personRepository.delete(id);
            userRepository.delete(id);
            AuditLogger.getAuditLogger(PersonsController.class).audit("Deleted person with id: " + id);
        }else{
            throw new ActionException("Forbidden action!");
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/update-person")
    @PreAuthorize("hasAuthority('UPDATE_PERSON')")
    public String updatePerson(Person person, @RequestParam("csrfToken") String csrfToken, HttpSession session) throws AccessDeniedException, ActionException {
        String csrf = session.getAttribute("CSRF_TOKEN").toString();
        if(!csrf.equals(csrfToken)){
            throw new AccessDeniedException("Invalid CSRF token");
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User usr = (User) auth.getPrincipal();
        int usrId = usr.getId();
        int id = Integer.parseInt(person.getId());
        boolean hasauth = usr.getAuthorities().contains(new SimpleGrantedAuthority("UPDATE_PERSON"));
        if(usrId == id || hasauth) {
            personRepository.update(person);
            AuditLogger.getAuditLogger(PersonsController.class).audit("Updated person with id: " + id);
        }else{
            throw new ActionException("Forbidden action!");
        }
        return "redirect:/persons/" + person.getId();
    }

    @GetMapping("/persons")
    @PreAuthorize("hasAuthority('VIEW_PERSONS_LIST')")
    public String persons(Model model, HttpSession session) {
        model.addAttribute("persons", personRepository.getAll());
        return "persons";
    }

    @GetMapping(value = "/persons/search", produces = "application/json")
    @ResponseBody
    public List<Person> searchPersons(@RequestParam String searchTerm) throws SQLException {
        return personRepository.search(searchTerm);
    }
}
