package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

public class CrawAction extends RecursiveAction {
    final String url;
    final Instant deadline;
    final Clock clock;
    final int maxDepth;
    final Map<String, Integer> counts;
    final Set<String> visitedUrls;
    private final List<Pattern> ignoredUrls;
    private final PageParserFactory parserFactory;

    CrawAction(String url,
               Instant deadline,
               Clock clock,
               int maxDepth,
               Map<String, Integer> counts,
               Set<String> visitedUrls,
               List<Pattern> ignoredUrls,
               PageParserFactory parserFactory) {
        this.url = url;
        this.deadline = deadline;
        this.clock = clock;
        this.maxDepth = maxDepth;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        this.ignoredUrls = ignoredUrls;
        this.parserFactory = parserFactory;
    }

    @Override
    protected void compute() {
        if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
            return;
        }
        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return;
            }
        }
        if (visitedUrls.contains(url)) {
            return;
        }
        synchronized (visitedUrls) {
            if(!visitedUrls.add(url)) {
                return;
            }
        }
        PageParser.Result result = parserFactory.get(url).parse();
        for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
            synchronized (counts) {
                if (counts.containsKey(e.getKey())) {
                    counts.put(e.getKey(), e.getValue() + counts.get(e.getKey()));
                } else {
                    counts.put(e.getKey(), e.getValue());
                }
            }
        }

        List<CrawAction> subActions = new ArrayList<>();
        for (String link : result.getLinks()) {
            subActions.add(new CrawAction(link, deadline, clock, maxDepth - 1, counts, visitedUrls, ignoredUrls, parserFactory));
        }
        invokeAll(subActions);
    }
}
