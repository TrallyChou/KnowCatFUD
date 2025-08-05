package life.trally.knowcatfud.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class hello {

    // 这是主页的测试资源
    @GetMapping({"/hello","/"})
    @PreAuthorize("hasAnyAuthority('files:list')")
    public String helloSpring(){
        return "Hello World";
    }
}
