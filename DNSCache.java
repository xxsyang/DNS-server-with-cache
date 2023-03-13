import java.util.HashMap;
public class DNSCache {
    HashMap<DNSQuestion, DNSRecord> cache;

    public DNSCache() {
        cache = new HashMap<>();
    }

    public DNSRecord querying(DNSQuestion question){
        if(cache.containsKey(question)){
            DNSRecord record = cache.get(question);
            if(record.isExpired()){
                return record;
            }
            else {
                cache.remove(question);
            }
        }
        return null;
    }

    public void insertRecord(DNSQuestion question, DNSRecord record) {
        cache.put(question, record);
    }

}
