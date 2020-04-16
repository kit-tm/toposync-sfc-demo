package toposync.demo.model;

public interface GUI extends StateObserver {
    void showError(String error);

    void topoSyncFetched();

    void shortestPathFetched();

    void reset();
}
