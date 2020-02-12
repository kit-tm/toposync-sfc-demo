package toposync.demo;

import toposync.demo.controller.Controller;
import toposync.demo.fetcher.GraphFetcher;
import toposync.demo.fetcher.OnosTopologyFetcher;
import toposync.demo.view.DemoUI;

import java.io.IOException;

public class Main {


    public static void main(String[] args) throws IOException {
        System.setProperty("org.graphstream.ui", "swing"); // tells GraphStream to use Swing
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");

        GraphFetcher fetcher = new OnosTopologyFetcher();


        DemoUI ui = new DemoUI();

        Controller controller = new Controller(ui, fetcher);

        ui.setController(controller);

        controller.fetchGraph();
    }
}
