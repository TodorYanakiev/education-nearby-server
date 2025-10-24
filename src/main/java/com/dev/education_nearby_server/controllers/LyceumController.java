package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.models.dto.request.LyceumRightsRequest;
import com.dev.education_nearby_server.services.LyceumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lyceums")
@RequiredArgsConstructor
public class LyceumController {

    private final LyceumService lyceumService;

    @PostMapping("/request-rights")
    public ResponseEntity<String> requestRightsOverLyceum (@Valid @RequestBody LyceumRightsRequest request) {
        return ResponseEntity.ok(lyceumService.requestRightsOverLyceum(request));
        /*Workflow:
        * 1) User enters name and town
        * 2.1) We try to find the lyceum by name and town
        * 2.2) If the lyceum is not found, we tell them sorry
        * 3.1) We send verification email to the lyceum address. If the lyceum's email is outdated, the user should contact us
        * 3.2) If the lyceum does not have email, we tell the user we will contact them
        * 4) We are waiting for the user to respond to the email. The verification will be in another request
        * Get done the /request-rights request in /controllers/Lyceum controller. The user should give lyceum name and town. We try to find the lyceum. Note that the user may ener the names
  in other cases or with or without quetes. If the lyceum is not found we tell them we are sorry and to contact us. If it is found, we send email to the lyceums email in our data
  base. Then we tell them we have sent them an emial to this emial address, if there is an outdated email, they should contact us. The sent email should contains token value which is
  the value of newly created Token entity with new TokenType - VERIFICATION. The token belongs to the user entity. Then we are waintg for the users response to our email, which is
  not your current task (saying just for context)
  *
  * Implemented the /request-rights flow end-to-end: input normalization, lyceum lookup, token creation, and email sending.

  What I Changed

  - Updated controller to accept JSON body:
      - src/main/java/com/dev/education_nearby_server/controllers/LyceumController.java:15
  - Added VERIFICATION token type:
      - src/main/java/com/dev/education_nearby_server/enums/TokenType.java:4
  - Added repository method to find lyceum by name + town (case-insensitive):
      - src/main/java/com/dev/education_nearby_server/repositories/LyceumRepository.java:8
  - Implemented service logic for the workflow (normalize inputs, find lyceum, create token, send email, return message):
      - src/main/java/com/dev/education_nearby_server/services/LyceumService.java:1
  - Added simple mail service using Spring Mail:
      - src/main/java/com/dev/education_nearby_server/services/EmailService.java:1

  Endpoint Behavior

  - Input: lyceumName and town (accepts any case). Spaces are trimmed/collapsed; quotes are preserved to match stored names.
  - If lyceum not found: returns “We are sorry, we could not find such lyceum. Please contact us.”
  - If found but email missing: returns “We could not reach the lyceum via email. Please contact us.”
  - If found with email:
      - Creates a new Token with TokenType.VERIFICATION, tokenValue = UUID, linked to the authenticated User.
      - Sends an email to the lyceum with the token value.
      - Returns: “We have sent them an email at <email>. If the email is outdated, please contact us.”

  Notes

  - The current user is resolved from SecurityContextHolder. If unauthenticated, it returns 401 via UnauthorizedException.
  - Email uses Spring Boot Mail auto-config; ensure spring.mail.* is configured in your application properties for actual delivery.
  - If you prefer passing Principal into the controller and down to the service instead of using SecurityContextHolder, I can wire that up. Would you like me to add a small
    integration test for this flow?

*/
    }
}
