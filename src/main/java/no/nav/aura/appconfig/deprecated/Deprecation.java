package no.nav.aura.appconfig.deprecated;

import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Marker class for a deprecated elements in appconfig xml
 * 
 * A deprecation will give warning until the from date is reached, then it will be an error
 *
 */
public class Deprecation {
    
    private Date from;
    private String tag;
    private String suggestion;
    
    
    /**
     * @param tag name of the deprecated element
     * @param from 
     */
    public Deprecation(String tag, Date from ) {
       this(tag, from, null);
    }
    
    /**
     * @param tag name of the deprecated element
     * @param from
     * @param suggestion what to replace it with
     */
    public Deprecation(String tag, Date from, String suggestion) {
        this.from = from;
        this.tag = tag;
        this.suggestion = suggestion;
    }
    
    public static Date date(int year, int month, int day){
        return new GregorianCalendar(year, month, day).getTime();
    }
    
    public boolean isExpired(){
        return from.before(new Date());
    }

    public Date getFrom() {
        return from;
    }

    public void setFrom(Date from) {
        this.from = from;
    }

    public String getTag() {
        return tag;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((tag == null) ? 0 : tag.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Deprecation other = (Deprecation) obj;
        if (tag == null) {
            if (other.tag != null)
                return false;
        } else if (!tag.equals(other.tag))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Deprecation [from=" + from + ", tag=" + tag + ", suggestion=" + suggestion + "]";
    }

}
