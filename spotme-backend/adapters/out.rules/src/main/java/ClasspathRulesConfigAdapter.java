import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotme.domain.port.RulesConfigPort;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class ClasspathRulesConfigAdapter implements RulesConfigPort {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public JsonNode loadRules(String version) {
        String path = "/rules/" + version + ".json";
        try (InputStream in = getClass().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("Rules file not found: " + path);
            }
            return mapper.readTree(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load rules JSON from " + path, e);
        }
    }
}
