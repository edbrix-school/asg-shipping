package com.asg.shipping.welcome;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/welcome")
public class WelcomeController {
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping
    public String welcome(){
        System.out.println( UserContext.getGroupPoid());
        return "Welcome";
    }
}
