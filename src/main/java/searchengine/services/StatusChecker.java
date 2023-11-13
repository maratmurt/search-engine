package searchengine.services;

import searchengine.model.SiteEntity;
import searchengine.model.Status;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class StatusChecker implements Runnable {
    private final CompletableFuture<SiteEntity> future;
    private final RepositoryService repositoryService;

    public StatusChecker(CompletableFuture<SiteEntity> future, RepositoryService repositoryService) {
        this.future = future;
        this.repositoryService = repositoryService;
    }

    @Override
    public void run() {
        try {
            SiteEntity site = future.get();
            site.setStatus(Status.INDEXED);
            repositoryService.save(site);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
