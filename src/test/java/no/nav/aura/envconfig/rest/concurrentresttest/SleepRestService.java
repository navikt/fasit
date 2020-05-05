package no.nav.aura.envconfig.rest.concurrentresttest;

import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Component
@Path("/conf/sleep")
public class SleepRestService {

    @GET
    public String sleep( @QueryParam("milliseconds") Integer milliseconds){
        try {
            Thread.sleep(milliseconds);
            return "I slept " + (double)milliseconds/1000 + " seconds, yay!";
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
