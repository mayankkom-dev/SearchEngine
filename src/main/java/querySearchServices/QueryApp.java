package querySearchServices;

import io.javalin.Javalin;
import io.javalin.http.Handler;
import java.util.*;
import static io.javalin.apibuilder.ApiBuilder.*;


public class QueryApp {

    public static void main(String[] args) {

        Javalin app = Javalin.create(config -> {
            config.enableCorsForAllOrigins();
        }).start("127.0.0.1",8878);
//        3.4.1
        app.routes(() -> {
            post("/search", SEARCH);
            post("/suggest", SUGGEST);
        });
    }
    private static final Handler SEARCH = (ctx) -> {
        List<QuerySearchResObj> results = new ArrayList();
        String queryTerm = ctx.formParam("queryTerm");
        boolean ngramSrch = (ctx.formParam("ngramSrch") != null);
        boolean wildCard = (ctx.formParam("wildCardSearch") != null);
        if (queryTerm != null && !queryTerm.isBlank()) {
            results = QuerySearchResult.searchIndex(queryTerm, ngramSrch, wildCard);
        }
        if (!results.isEmpty()) {
            ctx.json(results);
        } else {
            ctx.json(Collections.EMPTY_LIST);
        }
    };
    private static final Handler SUGGEST = (ctx) -> {
        List<SpellSuggestResObj> results = new ArrayList();
        String queryTerm = ctx.formParam("queryTerm");
        if (queryTerm != null && !queryTerm.isBlank()) {
            results = QuerySearchResult.spellsuggest(queryTerm);
        }
        if (!results.isEmpty()) {
            ctx.json(results);
        } else {
            ctx.json(Collections.EMPTY_LIST);
        }
    };
}
