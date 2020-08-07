import com.redislabs.lettusearch.RediSearchClient;
import com.redislabs.lettusearch.RediSearchCommands;
import com.redislabs.lettusearch.StatefulRediSearchConnection;
import com.redislabs.lettusearch.index.Schema;
import com.redislabs.lettusearch.index.field.TextField;
import com.redislabs.lettusearch.search.Document;
import com.redislabs.lettusearch.search.SearchResults;

/**
 * @author maxuefeng
 * @since 2020/6/15
 */
public class RedisSearch {


    public void searchKeywords() {
        RediSearchClient client = RediSearchClient.create("redis://114.67.246.62:6379");
        StatefulRediSearchConnection<String, String> connection = client.connect();
        RediSearchCommands<String, String> commands = connection.sync();

        commands.create("key_", Schema.builder().field(TextField.builder().name("NAME").build()).build()); //(4)
        commands.add("INDEX", Document.builder().id("ID").score(1D).field("NAME", "La Chouffe").build()); //(5)

        SearchResults<String, String> results = commands.search("INDEX", "chou*");

    }
}
