package test;

import com.github.laim0nas100.cfg.ConfigSettings;
import com.github.laim0nas100.cfg.ConfigSettings.MutableConfigSettings;
import com.github.laim0nas100.cfg.TolerantConfig;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/**
 *
 * @author laim0nas100
 */
public class Test1 {

    public static void main(String[] args) {
        Map<String, String> env = System.getenv();
        for (Map.Entry entry : env.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }

        Map<String, String> map = new HashMap();
        map.put("some.property.name", " John ");
        map.put("some.property.surname", " Cena ");
        map.put("some.property.greeting", "and his name is ${some.property.name} ${some.property.surname}");
        map.put("some.property2", "   ${some.property.greeting} ${env:temporary}   ");
        MutableConfigSettings settings = new MutableConfigSettings();
        settings.trimInterpolated = true;
        settings.strictMode = true;
        TolerantConfig conf = TolerantConfig.of(settings,map);
        System.out.println(conf.getStringTrim("some.property2"));
        System.out.println();
    }

    @Test
    public void testInterpolation() {
        Map<String, String> map = new HashMap();
        map.put("some.property.name", "John");
        map.put("some.property.surname", "Cena");
        map.put("some.property.greeting", "and his name is ${some.property.name} ${some.property.surname}");
        map.put("some.property2", "${some.property1} ${env:tmp}");
        
        TolerantConfig conf = TolerantConfig.of(map);
        String greeting = "and his name is John Cena";
        assert greeting.equals(conf.getString("some.property.greeting"));
        
        TolerantConfig subset = conf.subset("some.property");
        
        assert greeting.equals(subset.getString("greeting")); // eager interpolation should work even after subset
    }

}
