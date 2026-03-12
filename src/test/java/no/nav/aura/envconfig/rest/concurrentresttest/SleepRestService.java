package no.nav.aura.envconfig.rest.concurrentresttest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/conf/sleep")
public class SleepRestService {

    @GetMapping
    public String sleep( @RequestParam("milliseconds") Integer milliseconds){
        try {
            Thread.sleep(milliseconds);
            return "I slept " + (double)milliseconds/1000 + " seconds, yay!";
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
