package toposync.demo;

import toposync.demo.controller.Controller;
import toposync.demo.model.fetcher.OnosTopologyFetcher;
import toposync.demo.model.fetcher.OnosTreeFetcher;
import toposync.demo.model.fetcher.TopologyFetcher;
import toposync.demo.model.fetcher.TreeFetcher;
import toposync.demo.view.DemoUI;

public class Main {


    public static void main(String[] args) {
        System.setProperty("org.graphstream.ui", "swing"); // tells GraphStream to use Swing
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");

        DemoUI ui = new DemoUI();

        TopologyFetcher topologyFetcher = new OnosTopologyFetcher();
        TreeFetcher treeFetcher = new OnosTreeFetcher(ui);

        Controller controller = new Controller(ui, topologyFetcher, treeFetcher);

        ui.setController(controller);

        controller.fetchTopology();
        controller.fetchCurrentTree();
    }
}
