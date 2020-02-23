package toposync.demo;

import toposync.demo.controller.Controller;
import toposync.demo.model.fetcher.*;
import toposync.demo.view.DemoUI;

import java.io.IOException;

public class Main {


    public static void main(String[] args) throws IOException {
        System.setProperty("org.graphstream.ui", "swing"); // tells GraphStream to use Swing
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");

        DemoUI ui = new DemoUI();

        TopologyFetcher topologyFetcher = new OnosTopologyFetcher();
        TreeFetcher treeFetcher = new OnosTreeFetcher(ui);

        Controller controller = new Controller(ui, topologyFetcher, treeFetcher);

        ui.setController(controller);

        controller.fetchTopology();
    }
}
