package dev.skoleff.user_session_service.infrastructure.controller;

import dev.skoleff.user_session_service.domain.model.UserSession;
import dev.skoleff.user_session_service.application.SessionService;
import org.springframework.web.bind.annotation.*;


@RestController
public class SessionController {

    private final SessionService service;

    public SessionController(SessionService service) {
        this.service = service;
    }

    @PostMapping
    public void createSession(@RequestBody UserSession session) {
        service.createSession(session);
    }

    @GetMapping("/{id}")
    public UserSession getSession(@PathVariable("id") String sessionId) {
        return service.getSession(sessionId);
    }

    @PutMapping("/{id}/status")
    public void setStatus(@PathVariable("id") String sessionId, @RequestParam String status) {
        switch (status){
            case "AVAILABLE":
                service.setAvailable(sessionId);
                break;
            case "MATCHED":
                service.setMatched(sessionId);
                break;
            case "DISCONNECT":
                service.setDisconnected(sessionId);
                break;
            default:
                //no
        }
    }

    @DeleteMapping("/{id}")
    public void deleteSession(@PathVariable("id") String sessionId) {
        service.deleteSession(sessionId);
    }
}