package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
    private final Clock clock;
    private final Duration timeout;
    private final int popularWordCount;
    private final int maxDepth;
    private final ForkJoinPool pool;
    private final PageParserFactory parserFactory;
    private final List<Pattern> ignoredUrls;

    @Inject
    ParallelWebCrawler(
            Clock clock,
            @Timeout Duration timeout,
            PageParserFactory parserFactory,
            @MaxDepth int maxDepth,
            @PopularWordCount int popularWordCount,
            @TargetParallelism int threadCount,
            @IgnoredUrls List<Pattern> ignoredUrls) {
        this.clock = clock;
        this.timeout = timeout;
        this.parserFactory = parserFactory;
        this.maxDepth = maxDepth;
        this.popularWordCount = popularWordCount;
        this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
        this.ignoredUrls = ignoredUrls;
    }

    @Override
    public CrawlResult crawl(List<String> startingUrls) {
        Instant deadline = clock.instant().plus(timeout);
        Map<String, Integer> counts = Collections.synchronizedMap(new HashMap<>());
        Set<String> visitedUrls = Collections.synchronizedSet(new HashSet<>());
        for (String url : startingUrls) {
            pool.invoke(new CrawAction(url, deadline, clock, maxDepth, counts, visitedUrls, ignoredUrls, parserFactory));
        }

        if (counts.isEmpty()) {
            return new CrawlResult.Builder()
                    .setWordCounts(counts)
                    .setUrlsVisited(visitedUrls.size())
                    .build();
        }

        return new CrawlResult.Builder()
                .setWordCounts(WordCounts.sort(counts, popularWordCount))
                .setUrlsVisited(visitedUrls.size())
                .build();
    }

    @Override
    public int getMaxParallelism() {
        return Runtime.getRuntime().availableProcessors();
    }
}
