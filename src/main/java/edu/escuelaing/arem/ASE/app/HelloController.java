package edu.escuelaing.arem.ASE.app;

@Component
public class HelloController {
    @GetMapping("/hello")
    public static String index() {
        return "Greetings from Spring Boot!";
    }

    @GetMapping("/goodbye")
    public static String bye() {
        return "Goodbye from Spring Boot!";
    }
}
